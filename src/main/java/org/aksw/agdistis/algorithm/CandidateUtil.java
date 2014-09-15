package org.aksw.agdistis.algorithm;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.graph.Node;
import org.aksw.agdistis.util.Triple;
import org.aksw.agdistis.util.TripleIndex;
import org.apache.lucene.search.spell.NGramDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class CandidateUtil {
	private static Logger log = LoggerFactory.getLogger(CandidateUtil.class);
	private String nodeType;
	private TripleIndex index;
	private NGramDistance n;

	public CandidateUtil() {
		try {
			Properties prop = new Properties();
			InputStream input = new FileInputStream("agdistis.properties");
			prop.load(input);

			String nodeType = prop.getProperty("nodeType");
			String nGramDistance = prop.getProperty("ngramDistance");

			this.index = new TripleIndex();
			this.n = new NGramDistance(Integer.valueOf(nGramDistance));
			this.nodeType = nodeType;
		} catch (IOException e) {
			log.error("Cannot create CandidateUtil. AGDISTIS will not work so.", e);
		}
	}

	public void insertCandidatesIntoText(DirectedSparseGraph<Node, String> graph, Document document, double threshholdTrigram) {
		NamedEntitiesInText namedEntities = document.getNamedEntitiesInText();
		String text = document.DocumentText().getText();

		HashMap<String, Node> nodes = new HashMap<String, Node>();

		// used for heuristic label expansion start with longest Named Entities
		Collections.sort(namedEntities.getNamedEntities(), new NamedEntityLengthComparator());
		Collections.reverse(namedEntities.getNamedEntities());
		HashSet<String> heuristicExpansion = new HashSet<String>();
		for (NamedEntityInText entity : namedEntities) {
			String label = text.substring(entity.getStartPos(), entity.getEndPos());

			log.info("\tLabel: " + label);
			long start = System.currentTimeMillis();

			label = heuristicExpansion(heuristicExpansion, label);
			checkLabelCandidates(graph, threshholdTrigram, nodes, entity, label,false);

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
					log.debug("Heuristic expansion: " + label + "-->" + key);
				}
				if (tmp.length() < key.length() && tmp == label) {
					tmp = key;
					expansion = true;
					log.debug("Heuristic expansion: " + label + "-->" + key);
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
				log.error("This vertex couldn't be added because of an bug in Jung: " + candidateURL);
			} else {
				nodes.get(candidateURL).addId(st);
			}
		} else {
			currentNode.addId(entity.getStartPos());
			nodes.put(candidateURL, currentNode);
		}
	}

	public String redirect(String candidateURL) {
		if (candidateURL == null) {
			return candidateURL;
		}
		List<Triple> redirect = index.search(candidateURL, "http://dbpedia.org/ontology/wikiPageRedirects", null);
		if (redirect.size() == 1) {
			return redirect.get(0).getObject();
		} else if (redirect.size() > 1) {
			log.error("Candidate: " + candidateURL + " redirect.get(0).getObject(): " + redirect.get(0).getObject());
			return candidateURL;
		} else {
			return candidateURL;
		}
	}

	private void checkLabelCandidates(DirectedSparseGraph<Node, String> graph, double threshholdTrigram, HashMap<String, Node> nodes, NamedEntityInText entity, String label, boolean searchInSurfaceForms) {
		label = cleanLabelsfromCorporationIdentifier(label);
		label = label.trim();

		List<Triple> candidates = new ArrayList<Triple>();
		candidates = searchCandidatesByLabel(label, searchInSurfaceForms);
		log.info("\t\tnumber of candidates: " + candidates.size());
		if (candidates.size() == 0) {
			log.info("\t\t\tNo candidates for: " + label);
			if (label.endsWith("'s")) {
				// removing plural s
				label = label.substring(0, label.lastIndexOf("'s"));
				candidates = searchCandidatesByLabel(label, searchInSurfaceForms);
				log.info("\t\t\tEven not with expansion");
			} else if (label.endsWith("s")) {
				// removing genitiv s
				label = label.substring(0, label.lastIndexOf("s"));
				candidates = searchCandidatesByLabel(label, searchInSurfaceForms);
				log.info("\t\t\tEven not with expansion");
			}
		}
		boolean added = false;
		for (Triple c : candidates) {
			String candidateURL = c.getSubject();
			String surfaceForm = c.getObject();
			// rule of thumb: no year numbers in candidates
			if (candidateURL.startsWith(nodeType) && !candidateURL.matches("[0-9][0-9]")) {
				// REX: Range similarity for (s,o) and predicate range
				if (!fitsRangeOfPredicate(candidateURL, entity)) {
					continue;
				}
				// trigram similarity
				if (trigramForURLLabel(surfaceForm, label) < threshholdTrigram) {
					continue;
				}
				// iff it is a disambiguation resource, skip it
				if (isDisambiguationResource(candidateURL)) {
					continue;
				}
				// follow redirect
					candidateURL = redirect(candidateURL);
				if (fitsIntoDomain(candidateURL)) {
					addNodeToGraph(graph, nodes, entity, c, candidateURL);
					added = true;
				}
			}
		}
		if (!added && !searchInSurfaceForms)
			checkLabelCandidates(graph, threshholdTrigram, nodes, entity, label, true);
	}

	private ArrayList<Triple> searchCandidatesByLabel(String label, boolean searchInSurfaceFormsToo) {
		ArrayList<Triple> tmp = new ArrayList<Triple>();
		tmp.addAll(index.search(null, "http://www.w3.org/2000/01/rdf-schema#label", label));
		if (searchInSurfaceFormsToo) {
			tmp.addAll(index.search(null, "http://www.w3.org/2004/02/skos/core#altLabel", label));
		}
		return tmp;
	}

	private boolean isDisambiguationResource(String candidateURL) {
		List<Triple> tmp = index.search(candidateURL, "http://dbpedia.org/ontology/wikiPageDisambiguates", null);
		if (tmp.isEmpty())
			return false;
		else
			return true;
	}

	private String cleanLabelsfromCorporationIdentifier(String label) {
		//TODO put that in agdistis.properties as well
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

	private double trigramForURLLabel(String surfaceForm, String label) {
		return n.getDistance(surfaceForm, label);
	}

	private boolean fitsRangeOfPredicate(String candidateURL, NamedEntityInText entity) {
		if (entity.getDomain() == null)
			return true;
		HashSet<String> whiteList = new HashSet<String>();
		whiteList.add(entity.getDomain());

		List<Triple> tmp = index.search(candidateURL, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", null);
		if (tmp.isEmpty())
			return true;
		for (Triple triple : tmp) {
			if (!triple.getObject().contains("wordnet") && !triple.getObject().contains("wikicategory"))
				// log.debug("\ttype: " + triple.getObject());
				if (whiteList.contains(triple.getObject())) {
					return true;
				}
		}
		return false;
	}

	private boolean fitsIntoDomain(String candidateURL) {
		//TODO put that in properties file as well
		HashSet<String> whiteList = new HashSet<String>();
		if (nodeType.contains("dbpedia.org")) {
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

		List<Triple> tmp = index.search(candidateURL, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", null);
		if (tmp.isEmpty())
			return true;
		for (Triple triple : tmp) {
			if (!triple.getObject().contains("wordnet") && !triple.getObject().contains("wikicategory"))
				// log.debug("\ttype: " + triple.getObject());
				if (whiteList.contains(triple.getObject())) {
					return true;
				}
		}
		return false;
	}

	public String mapToDbpedia(String correctVotingURL) {
		List<Triple> mapping = index.search(correctVotingURL, "http://www.w3.org/2002/07/owl#sameAs", null);
		if (mapping.size() == 1) {
			return mapping.get(0).getObject();
		} else if (mapping.size() > 1) {
			log.error("More than one mapping" + correctVotingURL);
			return correctVotingURL;
		} else {
			return correctVotingURL;
		}
	}

	public void close() {
		try {
			index.close();
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
		}
	}

	public TripleIndex getIndex() {
		return index;
	}
}
