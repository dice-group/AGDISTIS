package org.aksw.agdistis.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.aksw.agdistis.graph.Node;
import org.aksw.agdistis.util.LabelURLIndex;
import org.aksw.agdistis.util.SubjectPredicateObjectIndex;
import org.aksw.agdistis.util.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datatypeshelper.utils.doc.Document;
import datatypeshelper.utils.doc.DocumentText;
import datatypeshelper.utils.doc.ner.NamedEntitiesInText;
import datatypeshelper.utils.doc.ner.NamedEntityInText;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class CandidateUtil {
	private static Logger log = LoggerFactory.getLogger(CandidateUtil.class);
	private String nodeType = null;
	private LabelURLIndex rdfsLabelIndex;
	private LabelURLIndex surfaceFormIndex;
	private SubjectPredicateObjectIndex index;
	private SubjectPredicateObjectIndex redirectIndex;
	private SubjectPredicateObjectIndex yago2dbpedia;

	/**
	 * @param knowledgeBase
	 *            "http://yago-knowledge.org/resource/" or
	 *            "http://dbpedia.org/resource/"
	 * 
	 * @param languageTag
	 *            en or de
	 * @param dataDirectory
	 *            parent directory of index and dump file directory. E.g.,
	 *            /data/r.usbeck ---> /data/r.usbeck/index/.., --->
	 *            /data/r.usbeck/dbpedia_[LANGUAGE]
	 */
	public CandidateUtil(String languageTag, String directory, String knowledgeBase) {
		if (knowledgeBase.equals("http://yago-knowledge.org/resource/")) {
			nodeType = "http://yago-knowledge.org/resource/";
			directory += "/yago";
			ArrayList<String> tmp = new ArrayList<String>();
			tmp.add(directory + "/yagoTypes.ttl");
			tmp.add(directory + "/yagoTransitiveType.ttl");
			tmp.add(directory + "/yagoFacts.ttl");
			tmp.add(directory + "/yagoLiteralFacts.ttl");
			this.index = new SubjectPredicateObjectIndex(tmp, directory + "/index/yagoOntology", nodeType, SubjectPredicateObjectIndex.TTL);

			String rdfsLabelFile = directory + "/yagoLabels.ttl";
			String rdfsLabelIndexDirectory = directory + "/index/yagoLabels";
			this.rdfsLabelIndex = new LabelURLIndex(rdfsLabelFile, rdfsLabelIndexDirectory, LabelURLIndex.TTL, nodeType);

			// no surface forms are given for yago
			// no redirects forms are given for yago

			tmp = new ArrayList<String>();
			tmp.add(directory + "/yagoDBpediaInstances.ttl");
			this.yago2dbpedia = new SubjectPredicateObjectIndex(tmp, directory + "/index/yago2dbpedia", nodeType, SubjectPredicateObjectIndex.TTL);
		} else {
			if (languageTag.equals("de")) {
				nodeType = "http://de.dbpedia.org/resource/";
			} else if (languageTag.equals("en")) {
				nodeType = "http://dbpedia.org/resource/";
			}
			String language = "dbpedia_" + languageTag;
			ArrayList<String> tmp = new ArrayList<String>();
			tmp.add(directory + "/instance_types_" + languageTag + ".nt");
			tmp.add(directory + "/mappingbased_properties_" + languageTag + ".nt");
			tmp.add(directory + "/specific_mappingbased_properties_" + languageTag + ".nt");
			tmp.add(directory + "/disambiguations_" + languageTag + ".nt");
			this.index = new SubjectPredicateObjectIndex(tmp, directory + "/index" + language + "/dbpediaOntology", nodeType, SubjectPredicateObjectIndex.N_TRIPLES);

			String rdfsLabelFile = directory + "/labels_" + languageTag + ".nt";
			String rdfsLabelIndexDirectory = directory + "/index" + language + "/label_rdfs_label";
			this.rdfsLabelIndex = new LabelURLIndex(rdfsLabelFile, rdfsLabelIndexDirectory, LabelURLIndex.N_TRIPLES, nodeType);

			String surfaceFormsFile = directory + "/" + languageTag + "_surface_forms.tsv";
			// hack for not using surfaceforms
			// String surfaceFormsFile = directory + "/labels_" + languageTag +
			// ".nt";

			String surfaceFormsIndexDirectory = directory + "/index" + language + "/label_surface";
			this.surfaceFormIndex = new LabelURLIndex(surfaceFormsFile, surfaceFormsIndexDirectory, LabelURLIndex.N_TRIPLES, nodeType);

			tmp = new ArrayList<String>();
			tmp.add(directory + "/redirects_transitive_" + languageTag + ".nt");
			this.redirectIndex = new SubjectPredicateObjectIndex(tmp, directory + "/index" + language + "/dbpediaOntologyRedirects", nodeType, SubjectPredicateObjectIndex.N_TRIPLES);
		}

	}

	public CandidateUtil(String modelDirectory, String knowledgeBase) {
		if (knowledgeBase.equals("http://yago-knowledge.org/resource/")) {
			nodeType = "http://yago-knowledge.org/resource/";
			this.index = new SubjectPredicateObjectIndex(modelDirectory + "/yagoOntology");
			this.rdfsLabelIndex = new LabelURLIndex(modelDirectory + "/yagoLabels");
			this.yago2dbpedia = new SubjectPredicateObjectIndex(modelDirectory + "/yago2dbpedia");
			// no surface forms are given for yago
			// no redirects forms are given for yago
		} else {
			nodeType = "http://dbpedia.org/resource/";
			this.index = new SubjectPredicateObjectIndex(modelDirectory + "/dbpediaOntology");
			this.rdfsLabelIndex = new LabelURLIndex(modelDirectory + "/label_rdfs_label");
			this.surfaceFormIndex = new LabelURLIndex(modelDirectory + "/label_surface");
			this.redirectIndex = new SubjectPredicateObjectIndex(modelDirectory + "/dbpediaOntologyRedirects");
		}
	}

	public void insertCandidatesIntoText(DirectedSparseGraph<Node, String> graph, Document document, double threshholdTrigram) {
		NamedEntitiesInText namedEntities = document.getProperty(NamedEntitiesInText.class);
		String text = document.getProperty(DocumentText.class).getText();
		HashMap<String, Node> nodes = new HashMap<String, Node>();

		// start with longest Named Entities
		Collections.sort(namedEntities.getNamedEntities(), new NamedEntityLengthComparator());
		Collections.reverse(namedEntities.getNamedEntities());
		HashSet<String> heuristicExpansion = new HashSet<String>();
		for (NamedEntityInText entity : namedEntities) {
			String label = text.substring(entity.getStartPos(), entity.getEndPos());
			log.info("\tLabel: " + label);
			long start = System.currentTimeMillis();
			label = heuristicExpansion(heuristicExpansion, label);
			if (nodeType.equals("http://yago-knowledge.org/resource/")) {
				checkRdfsLabelCandidates(graph, threshholdTrigram, nodes, entity, label, nodeType);
			} else {
//				checkRdfsLabelCandidates(graph, threshholdTrigram, nodes, entity, label, "http://dbpedia.org/resource/");
				if (!checkRdfsLabelCandidates(graph, threshholdTrigram, nodes, entity, label, "http://dbpedia.org/resource/"))
					checkSurfaceFormsCandidates(graph, nodes, threshholdTrigram, entity, label, "http://dbpedia.org/resource/");
			}
			log.info("\tGraph size: " + graph.getVertexCount() + " took: " + (System.currentTimeMillis() - start) + " ms");
		}
	}

	private String heuristicExpansion(HashSet<String> heuristicExpansion, String label) {
		String tmp = label;
		boolean expansion = false;
		for (String key : heuristicExpansion) {
			if (key.contains(label)) {
				// take the shortest possible expansion
				if (tmp.length() > key.length() && tmp != label) {
					tmp = key;
					expansion = true;
					log.debug("Heuristik expansion: " + label + "-->" + key);
				}
				if (tmp.length() < key.length() && tmp == label) {
					tmp = key;
					expansion = true;
					log.debug("Heuristik expansion: " + label + "-->" + key);
				}
			}
		}
		label = tmp;
		if (!expansion) {
			heuristicExpansion.add(label);
		}
		return label;
	}

	public void addNodeToGraph(DirectedSparseGraph<Node, String> graph, HashMap<String, Node> nodes, NamedEntityInText entity, Triple c, String candidateURL) {
		Node currentNode = new Node(candidateURL, 0, 0);
		log.debug(currentNode.toString());
		// candidates are connected to a specific label in the text via their
		// start position
		if (!graph.addVertex(currentNode)) {
			int st = entity.getStartPos();
			if (nodes.get(candidateURL) == null) {
				// no more jung is used so maybe this error does not occure
				// anymore
				log.error("This vertex couldn't be added because of an bug in Jung: " + candidateURL);
			} else {
				nodes.get(candidateURL).addId(st);
				log.debug("\t\tCandidate has not been insert: " + c + " but inserted an additional labelId at that node.");
			}
		} else {
			currentNode.addId(entity.getStartPos());
			nodes.put(candidateURL, currentNode);
		}
	}

	public void close() {
		try {
			index.close();
			redirectIndex.close();
			rdfsLabelIndex.close();
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
		}
	}

	public SubjectPredicateObjectIndex getIndex() {
		return index;
	}

	public String redirect(String candidateURL) {
		List<Triple> redirect = redirectIndex.search(candidateURL);
		if (redirect.size() == 1) {
			return redirect.get(0).getObject();
		} else if (redirect.size() > 1) {
			log.error("More than one transitive redirect" + candidateURL);
			return candidateURL;
		} else {
			return candidateURL;
		}
	}

	private boolean checkSurfaceFormsCandidates(DirectedSparseGraph<Node, String> graph, HashMap<String, Node> nodes, double threshholdTrigram, NamedEntityInText entity, String label, String knowledgeBase) {
		boolean addedCandidates = false;
		label = cleanLabelsfromCorporationIdentifier(label);
		label = label.trim();

		List<Triple> candidates = surfaceFormIndex.searchInLabels(label, true);
		if (candidates.size() == 0) {
			log.info("\t\t\tNo candidates for: " + label);
			if (label.endsWith("'s")) {
				// removing plural s
				label = label.substring(0, label.lastIndexOf("'s"));
				candidates = surfaceFormIndex.searchInLabels(label, true);
				log.info("\t\t\tEven not with expansion");
			} else if (label.endsWith("s")) {
				// removing genitiv s
				label = label.substring(0, label.lastIndexOf("s"));
				candidates = surfaceFormIndex.searchInLabels(label, true);
				log.info("\t\t\tEven not with expansion");
			}
		}
		for (Triple c : candidates) {
			String candidateURL = c.getSubject();
			log.debug("\tcand: " + candidateURL);
			// rule of thumb: no year numbers in candidates
			if (candidateURL.startsWith(nodeType) && !candidateURL.matches("[0-9][0-9]")) {
				// iff it is a disambiguation resource, skip it
				if (disambiguates(candidateURL) != candidateURL) {
					continue;
				}
				// trigram similarity
				if (trigramLabelLabel(c.getObject(), label, nodeType) < threshholdTrigram) {
					continue;
				}
				// follow redirect
				if (!nodeType.equals("http://yago-knowledge.org/resource/")) {
					candidateURL = redirect(candidateURL);
				}
				if (fitsIntoDomain(candidateURL, knowledgeBase)) {
					addNodeToGraph(graph, nodes, entity, c, candidateURL);
					addedCandidates = true;
				}
			}
		}
		return addedCandidates;
	}

	private boolean checkRdfsLabelCandidates(DirectedSparseGraph<Node, String> graph, double threshholdTrigram, HashMap<String, Node> nodes, NamedEntityInText entity, String label, String knowledgeBase) {
		boolean addedCandidates = false;
		label = cleanLabelsfromCorporationIdentifier(label);
		label = label.trim();

		List<Triple> candidates = rdfsLabelIndex.searchInLabels(label, false);
		log.info("\t\tnumber of candidates: " + candidates.size());
		if (candidates.size() == 0) {
			log.info("\t\t\tNo candidates for: " + label);
			if (label.endsWith("'s")) {
				// removing plural s
				label = label.substring(0, label.lastIndexOf("'s"));
				candidates = rdfsLabelIndex.searchInLabels(label, false);
				log.info("\t\t\tEven not with expansion");
			} else if (label.endsWith("s")) {
				// removing genitiv s
				label = label.substring(0, label.lastIndexOf("s"));
				candidates = rdfsLabelIndex.searchInLabels(label, false);
				log.info("\t\t\tEven not with expansion");
			}
		}
		for (Triple c : candidates) {
			String candidateURL = c.getSubject();
			// rule of thumb: no year numbers in candidates
			if (candidateURL.startsWith(nodeType) && !candidateURL.matches("[0-9][0-9]")) {
				// trigram similarity
				if (trigramForURLLabel(candidateURL, label, nodeType) < threshholdTrigram) {
					continue;
				}
				// iff it is a disambiguation resource, skip it
				if (disambiguates(candidateURL) != candidateURL) {
					continue;
				}
				// follow redirect
				if (!nodeType.equals("http://yago-knowledge.org/resource/")) {
					candidateURL = redirect(candidateURL);
				}
				if (fitsIntoDomain(candidateURL, knowledgeBase)) {
					addNodeToGraph(graph, nodes, entity, c, candidateURL);
					addedCandidates = true;
				}
			}
		}
		return addedCandidates;
	}

	private String disambiguates(String candidateURL) {
		List<Triple> tmp = index.search(candidateURL);
		if (tmp.isEmpty())
			return candidateURL;
		for (Triple triple : tmp) {
			String predicate = triple.getPredicate();
			if (predicate.equals("http://dbpedia.org/ontology/wikiPageDisambiguates")) {
				return triple.getObject();
			}
		}
		return candidateURL;
	}

	private String cleanLabelsfromCorporationIdentifier(String label) {
		if (label.endsWith("corp")) {
			label = label.substring(0, label.lastIndexOf("corp"));
		} else if (label.endsWith("Corp")) {
			label = label.substring(0, label.lastIndexOf("Corp"));
		} else if (label.endsWith("ltd")) {
			label = label.substring(0, label.lastIndexOf("ltd"));
		} else if (label.endsWith("Ltd")) {
			label = label.substring(0, label.lastIndexOf("Ltd"));
		} else if (label.endsWith("inc")) {
			label = label.substring(0, label.lastIndexOf("inc"));
		} else if (label.endsWith("Inc")) {
			label = label.substring(0, label.lastIndexOf("Inc"));
		} else if (label.endsWith("Co")) {
			label = label.substring(0, label.lastIndexOf("Co"));
		} else if (label.endsWith("co")) {
			label = label.substring(0, label.lastIndexOf("co"));
		}

		return label.trim();
	}

	private double trigramLabelLabel(String candidateLabel, String label, String nodeType) {
		HashSet<String> trigramsForLabel = new HashSet<String>();
		for (int i = 3; i < label.length(); i++) {
			trigramsForLabel.add(label.substring(i - 3, i).toLowerCase());
		}

		HashSet<String> trigramsForCandidate = new HashSet<String>();
		for (int i = 3; i < candidateLabel.length(); i++) {
			trigramsForCandidate.add(candidateLabel.substring(i - 3, i).toLowerCase());
		}
		HashSet<String> union = new HashSet<String>();
		union.addAll(trigramsForLabel);
		union.addAll(trigramsForCandidate);
		trigramsForLabel.retainAll(trigramsForCandidate);
		log.debug("\t\tcandidate: " + candidateLabel + " => orig: " + label + "=" + (double) trigramsForLabel.size() / ((double) union.size()));
		return (double) trigramsForLabel.size() / ((double) union.size());
	}

	private double trigramForURLLabel(String candidateURL, String label, String nodeType) {
		double sim = 0;
		if (!nodeType.equals("http://yago-knowledge.org/resource/")) {
			List<Triple> labelOfCandidate = rdfsLabelIndex.getLabelForURI(candidateURL);
			if (labelOfCandidate.isEmpty()) {
				return 0;
			}
			HashSet<String> union = new HashSet<String>();

			// ensure that there is one maximum matching label
			for (Triple t : labelOfCandidate) {
				HashSet<String> trigramsForLabel = new HashSet<String>();
				for (int i = 3; i < label.length(); i++) {
					trigramsForLabel.add(label.substring(i - 3, i).toLowerCase());
				}
				union = new HashSet<String>();
				String replace = t.getObject().replace(nodeType, "").toLowerCase();
				replace = replace.replace("&", "and");
				HashSet<String> trigramsForCandidate = new HashSet<String>();
				for (int i = 3; i < replace.length(); i++) {
					trigramsForCandidate.add(replace.substring(i - 3, i).toLowerCase());
				}
				union.addAll(trigramsForLabel);
				union.addAll(trigramsForCandidate);
				trigramsForLabel.retainAll(trigramsForCandidate);
				// log.debug("\t\tcandidate: " + replace + " => orig: " + label
				// + "=" + sim);
				double tmp = (double) trigramsForLabel.size() / ((double) union.size());
				if (sim < tmp)
					sim = tmp;
			}
			return sim;
		} else {
			HashSet<String> trigramsForLabel = new HashSet<String>();
			for (int i = 3; i < label.length(); i++) {
				trigramsForLabel.add(label.substring(i - 3, i).toLowerCase());
			}
			List<Triple> labelOfCandidate = rdfsLabelIndex.getLabelForURI(candidateURL);
			if (labelOfCandidate.isEmpty()) {
				return 0;
			}
			String replace = labelOfCandidate.get(0).getObject().replace(nodeType, "").toLowerCase();
			replace = replace.replace("&", "and");
			HashSet<String> trigramsForCandidate = new HashSet<String>();
			for (int i = 3; i < replace.length(); i++) {
				trigramsForCandidate.add(replace.substring(i - 3, i).toLowerCase());
			}
			HashSet<String> union = new HashSet<String>();
			union.addAll(trigramsForLabel);
			union.addAll(trigramsForCandidate);
			trigramsForLabel.retainAll(trigramsForCandidate);
			log.debug("\t\tcandidate: " + replace + " => orig: " + label + "=" + (double) trigramsForLabel.size() / ((double) union.size()));
			return (double) trigramsForLabel.size() / ((double) union.size());
		}
	}

	private boolean fitsIntoDomain(String candidateURL, String knowledgeBase) {
		HashSet<String> whiteList = new HashSet<String>();
		if ("http://dbpedia.org/resource/".equals(knowledgeBase)) {
			whiteList.add("http://dbpedia.org/ontology/Place");
			whiteList.add("http://dbpedia.org/ontology/Person");
			whiteList.add("http://dbpedia.org/ontology/Organisation");
			whiteList.add("http://dbpedia.org/class/yago/YagoGeoEntity");
			whiteList.add("http://xmlns.com/foaf/0.1/Person");
			whiteList.add("http://dbpedia.org/ontology/WrittenWork");
		} else {
			whiteList.add("http://yago-knowledge.org/resource/yagoGeoEntity");
			whiteList.add("http://yago-knowledge.org/resource/yagoLegalActor");
			whiteList.add("http://yago-knowledge.org/resource/wordnet_exchange_111409538");
		}

		List<Triple> tmp = index.search(candidateURL);
		if (tmp.isEmpty())
			return true;
		for (Triple triple : tmp) {
			String predicate = triple.getPredicate();
			if (predicate.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
				if (!triple.getObject().contains("wordnet") && !triple.getObject().contains("wikicategory"))
					log.debug("\ttype: " + triple.getObject());
				if (whiteList.contains(triple.getObject())) {
					return true;
				}
			}
		}
		return false;
	}

	public String mapToDbpedia(String correctVotingURL) {
		List<Triple> mapping = yago2dbpedia.search(correctVotingURL);
		if (mapping.size() == 1) {
			return mapping.get(0).getObject();
		} else if (mapping.size() > 1) {
			log.error("More than one mapping" + correctVotingURL);
			return correctVotingURL;
		} else {
			return correctVotingURL;
		}
	}

}
