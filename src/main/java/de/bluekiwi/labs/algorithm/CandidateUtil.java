package de.bluekiwi.labs.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unister.semweb.topicmodeling.utils.doc.Document;
import com.unister.semweb.topicmodeling.utils.doc.DocumentText;
import com.unister.semweb.topicmodeling.utils.doc.ner.NamedEntitiesInText;
import com.unister.semweb.topicmodeling.utils.doc.ner.NamedEntityInText;

import de.bluekiwi.labs.util.LabelURLIndex;
import de.bluekiwi.labs.util.SubjectPredicateObjectIndex;
import de.bluekiwi.labs.util.Triple;
import de.bluekiwi.labs.vis.MyNode;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class CandidateUtil {
    private static Logger log = LoggerFactory.getLogger(CandidateUtil.class);
    String nodeType = null;
    LabelURLIndex rdfsLabelIndex;
    LabelURLIndex surfaceFormIndex;
    SubjectPredicateObjectIndex index;
    SubjectPredicateObjectIndex redirectIndex;

    /**
     * 
     * @param languageTag
     *            en or de
     * @param dataDirectory
     *            parent directory of index and dump file directory.
     *            E.g., /data/r.usbeck
     *            ---> /data/r.usbeck/index/..,
     *            ---> /data/r.usbeck/dbpedia_[LANGUAGE]
     */
    public CandidateUtil(String languageTag, String dataDirectory) {
        if (languageTag.equals("de")) {
            nodeType = "http://de.dbpedia.org/resource/";
        } else if (languageTag.equals("en")) {
            nodeType = "http://dbpedia.org/resource/";
        }
        String directory = dataDirectory; // "/data/r.usbeck";
        String language = "dbpedia_" + languageTag;
        ArrayList<String> tmp = new ArrayList<String>();
        tmp.add(directory + "/" + language + "/instance_types_" + languageTag + ".nt");
        tmp.add(directory + "/" + language + "/mappingbased_properties_" + languageTag + ".nt");
        tmp.add(directory + "/" + language + "/specific_mappingbased_properties_" + languageTag + ".nt");
        tmp.add(directory + "/" + language + "/disambiguations_" + languageTag + ".nt");
        this.index = new SubjectPredicateObjectIndex(tmp, directory + "/index" + language + "/dbpediaOntology");

        String rdfsLabelFile = directory + "/" + language + "/labels_" + languageTag + ".nt";
        String rdfsLabelIndexDirectory = directory + "/index" + language + "/label_rdfs_label";
        this.rdfsLabelIndex = new LabelURLIndex(rdfsLabelFile, rdfsLabelIndexDirectory, LabelURLIndex.N_TRIPLES);

        String surfaceFormsFile = directory + "/" + language + "/" + languageTag + "_surface_forms.tsv";
        String surfaceFormsIndexDirectory = directory + "/index" + language + "/label_surface";
        this.surfaceFormIndex = new LabelURLIndex(surfaceFormsFile, surfaceFormsIndexDirectory, LabelURLIndex.TSV);

        tmp = new ArrayList<String>();
        tmp.add(directory + "/" + language + "/redirects_transitive_" + languageTag + ".nt");
        this.redirectIndex = new SubjectPredicateObjectIndex(tmp, directory + "/index" + language + "/dbpediaOntologyRedirects");

    }

    public void insertCandidatesIntoText(DirectedSparseGraph<MyNode, String> graph, Document document, double threshholdTrigram) {
        NamedEntitiesInText namedEntities = document.getProperty(NamedEntitiesInText.class);
        String text = document.getProperty(DocumentText.class).getText();

        HashSet<String> heuristicExpansion = new HashSet<String>();

        // start with longest Named Entities
        Collections.sort(namedEntities.getNamedEntities(), new NamedEntityLengthComparator());
        Collections.reverse(namedEntities.getNamedEntities());
        HashMap<String, MyNode> nodes = new HashMap<String, MyNode>();
        for (NamedEntityInText entity : namedEntities) {
            String label = text.substring(entity.getStartPos(), entity.getEndPos());
            log.info(label);
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
            if (!checkRdfsLabelCandidates(graph, threshholdTrigram, nodes, entity, label))
                checkSurfaceFormsCandidates(graph, nodes, threshholdTrigram, entity, label);
        }
    }

    public void addNodeToGraph(DirectedSparseGraph<MyNode, String> graph, HashMap<String, MyNode> nodes, NamedEntityInText entity, Triple c, String candidateURL) {
        MyNode currentNode = new MyNode(candidateURL, 0, 0);
        log.debug(currentNode.toString());
        // candidates are connected to a specific label in the text via their start position
        if (!graph.addVertex(currentNode)) {
            int st = entity.getStartPos();
            if (nodes.get(candidateURL) == null) {
                /*
                 * TODO jung bug obviously jung hashcodes colide, although the node doesn't exist it can't be added
                 */
                log.error("This vertex couldn't be added because of an bug in Jung: "
                        + candidateURL);
            } else {
                nodes.get(candidateURL).addId(st);
                log.debug("\t\tCandidate has not been insert: " + c
                        + " but inserted an additional labelId at that node.");
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

    private boolean checkSurfaceFormsCandidates(DirectedSparseGraph<MyNode, String> graph, HashMap<String, MyNode> nodes, double threshholdTrigram, NamedEntityInText entity, String label) {
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
                candidateURL = redirect(candidateURL);
                if (fitsIntoDomain(candidateURL)) {
                    addNodeToGraph(graph, nodes, entity, c, candidateURL);
                    addedCandidates = true;
                }
            }
        }
        return addedCandidates;
    }

    private boolean checkRdfsLabelCandidates(DirectedSparseGraph<MyNode, String> graph, double threshholdTrigram, HashMap<String, MyNode> nodes, NamedEntityInText entity, String label) {
        boolean addedCandidates = false;
        label = cleanLabelsfromCorporationIdentifier(label);
        label = label.trim();

        List<Triple> candidates = rdfsLabelIndex.searchInLabels(label, false);
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
                // iff it is a disambiguation resource, skip it
                if (disambiguates(candidateURL) != candidateURL) {
                    continue;
                }
                // trigram similarity
                if (trigramForURLLabel(candidateURL, label, nodeType) < threshholdTrigram) {
                    continue;
                }
                // follow redirect
                candidateURL = redirect(candidateURL);
                if (fitsIntoDomain(candidateURL)) {
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
            if (predicate
                    .equals("http://dbpedia.org/ontology/wikiPageDisambiguates")) {
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

    private boolean fitsIntoDomain(String candidateURL) {
        HashSet<String> whiteList = new HashSet<String>();
        whiteList.add("http://dbpedia.org/ontology/Place");
        whiteList.add("http://dbpedia.org/ontology/Person");
        whiteList.add("http://dbpedia.org/ontology/Organisation");
        whiteList.add("http://dbpedia.org/class/yago/YagoGeoEntity");
        whiteList.add("http://xmlns.com/foaf/0.1/Person");
        whiteList.add("http://dbpedia.org/ontology/WrittenWork");
        List<Triple> tmp = index.search(candidateURL);
        if (tmp.isEmpty())
            return true;
        for (Triple triple : tmp) {
            String predicate = triple.getPredicate();
            if (predicate.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                if (whiteList.contains(triple.getObject())) {
                    return true;
                }
            }
        }
        return false;
    }

}
