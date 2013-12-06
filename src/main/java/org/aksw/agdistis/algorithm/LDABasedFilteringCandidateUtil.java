package org.aksw.agdistis.algorithm;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.aksw.agdistis.graph.Node;
import org.aksw.agdistis.graph.NodeConfigurator;
import org.aksw.agdistis.util.Triple;
import org.aksw.agdistis.util.TripleIndex;
import org.apache.lucene.search.spell.NGramDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.mallet.pipe.Pipe;
import cc.mallet.topics.LongBasedTopicInferencer;
import cc.mallet.types.Instance;

import com.unister.semweb.commons.collections.associative.TopDoubleObjectCollection;
import com.unister.semweb.topicmodeling.preprocessing.SingleDocumentPreprocessor;

import datatypeshelper.utils.doc.Document;
import datatypeshelper.utils.doc.DocumentText;
import datatypeshelper.utils.doc.ner.NamedEntitiesInText;
import datatypeshelper.utils.doc.ner.NamedEntityInText;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class LDABasedFilteringCandidateUtil {
    private static Logger log = LoggerFactory.getLogger(LDABasedFilteringCandidateUtil.class);

    private static final int INFERENCER_ITERATIONS = 500;
    private static final int INFERENCER_BURN_IN = 200;
    // thinning is the number of iterations until the next sample is used for the result
    private static final int INFERENCER_THINNING = 50;

    private static final String ABSTRACT_PREDICAT = "http://dbpedia.org/ontology/abstract";

    private String nodeType = "http://dbpedia.org/resource/";
    private TripleIndex index;
    private NGramDistance n;
    private SingleDocumentPreprocessor preprocessor;
    private LongBasedTopicInferencer inferencer;
    private Pipe pipe;
    private int maxCandidatesAfterTM = 100;

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
    public LDABasedFilteringCandidateUtil(File indexDirectory, SingleDocumentPreprocessor preprocessor,
            LongBasedTopicInferencer inferencer, Pipe pipe) {
        index = new TripleIndex(indexDirectory);
        n = new NGramDistance(3);
        this.preprocessor = preprocessor;
        this.inferencer = inferencer;
        this.pipe = pipe;
    }

    public void setMaxCandidatesAfterTM(int maxCandidatesAfterTM) {
        this.maxCandidatesAfterTM = maxCandidatesAfterTM;
    }

    public void insertCandidatesIntoText(DirectedSparseGraph<Node, String> graph, Document document,
            double threshholdTrigram) {
        insertCandidatesIntoText(graph, document, threshholdTrigram, null);
    }

    public void insertCandidatesIntoText(DirectedSparseGraph<Node, String> graph, Document document,
            double threshholdTrigram, NodeConfigurator configurator) {
        NamedEntitiesInText namedEntities = document.getProperty(NamedEntitiesInText.class);
        String text = document.getProperty(DocumentText.class).getText();
        HashMap<String, Node> nodes = new HashMap<String, Node>();

        double textVector[] = inferencer
                .getSampledDistribution(preprocess(text), INFERENCER_ITERATIONS, INFERENCER_THINNING,
                        INFERENCER_BURN_IN);
        double textVectorLength = getVectorLength(textVector);

        // start with longest Named Entities
        Collections.sort(namedEntities.getNamedEntities(), new NamedEntityLengthComparator());
        Collections.reverse(namedEntities.getNamedEntities());
        HashSet<String> heuristicExpansion = new HashSet<String>();
        for (NamedEntityInText entity : namedEntities) {
            String label = text.substring(entity.getStartPos(), entity.getEndPos());
            log.info("\tLabel: " + label);
            long start = System.currentTimeMillis();
            label = heuristicExpansion(heuristicExpansion, label);
            checkLabelCandidates(graph, threshholdTrigram, nodes, entity, label, nodeType, configurator, textVector,
                    textVectorLength, true);
            log.info("\tGraph size: " + graph.getVertexCount() + " took: " + (System.currentTimeMillis() - start)
                    + " ms");
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

    public void addNodeToGraph(DirectedSparseGraph<Node, String> graph, HashMap<String, Node> nodes,
            NamedEntityInText entity, Triple c, String candidateURL, NodeConfigurator configurator) {
        Node currentNode = new Node(candidateURL, 0, 0);
        if (configurator != null) {
            configurator.configureNode(currentNode);
        }
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
                log.debug("\t\tCandidate has not been insert: " + c
                        + " but inserted an additional labelId at that node.");
            }
        } else {
            currentNode.addId(entity.getStartPos());
            nodes.put(candidateURL, currentNode);
        }
    }

    public void addNodeToGraph(DirectedSparseGraph<Node, String> graph, HashMap<String, Node> nodes,
            NamedEntityInText entity, Triple c, String candidateURL) {
        addNodeToGraph(graph, nodes, entity, c, candidateURL, null);
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

    private void checkLabelCandidates(DirectedSparseGraph<Node, String> graph, double threshholdTrigram,
            HashMap<String, Node> nodes, NamedEntityInText entity, String label, String knowledgeBase,
            NodeConfigurator configurator, double textVector[], double textVectorLength, boolean searchInSurfaceForms) {
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

        TopDoubleObjectCollection<Triple> topCandidates = new TopDoubleObjectCollection<Triple>(maxCandidatesAfterTM,
                false);
        double entityVector[];
        String entityText;
        for (Triple c : candidates) {
            String candidateURL = c.getSubject();
            String surfaceForm = c.getObject();
            // rule of thumb: no year numbers in candidates
            if (candidateURL.startsWith(nodeType) && !candidateURL.matches("[0-9][0-9]")) {
                // trigram similarity
                if (trigramForURLLabel(surfaceForm, label) < threshholdTrigram) {
                    continue;
                }
                // System.out.println(label + " -> " + surfaceForm + " : " + candidateURL);
                // iff it is a disambiguation resource, skip it
                if (isDisambiguationResource(candidateURL)) {
                    continue;
                }
                // follow redirect
                if (!nodeType.equals("http://yago-knowledge.org/resource/")) {
                    candidateURL = redirect(candidateURL);
                }
                if (fitsIntoDomain(candidateURL, knowledgeBase)) {
                    // get the abstract of the entity
                    entityText = getAbstract(candidateURL);
                    if (entityText != null) {
                        entityVector = inferencer.getSampledDistribution(preprocess(entityText), INFERENCER_ITERATIONS,
                                INFERENCER_THINNING, INFERENCER_BURN_IN);
                        topCandidates.add(getSimilarity(textVector, textVectorLength, entityVector,
                                getVectorLength(entityVector)), c);
                    }
                }
            }
        }

        Triple t;
        for (int i = 0; i < topCandidates.size(); ++i) {
            t = (Triple) topCandidates.objects[i];
            addNodeToGraph(graph, nodes, entity, t, t.getSubject(), configurator);
        }

        if ((topCandidates.size() == 0) && !searchInSurfaceForms)
            checkLabelCandidates(graph, threshholdTrigram, nodes, entity, label, nodeType, configurator, textVector,
                    textVectorLength, true);
    }

    private ArrayList<Triple> searchCandidatesByLabel(String label, boolean searchInSurfaceFormsToo) {
        ArrayList<Triple> tmp = new ArrayList<Triple>();
        tmp.addAll(index.search(null, "http://www.w3.org/2000/01/rdf-schema#label", label));
        if (searchInSurfaceFormsToo) {
            tmp.addAll(index.search(null, "http://www.w3.org/2004/02/skos/core#altLabel", label));
            tmp.addAll(index.search(null, "http://xmlns.com/foaf/0.1/name", label));
        }
        for (Triple t : tmp) {
            log.debug(label + " -> " + t.getObject());
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

        List<Triple> tmp = index.search(candidateURL, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", null);
        if (tmp.isEmpty())
            return true;
        for (Triple triple : tmp) {
            if (!triple.getObject().contains("wordnet") && !triple.getObject().contains("wikicategory"))
                log.debug("\ttype: " + triple.getObject());
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

    public double[] getTextVector(String text) {
        return inferencer.getSampledDistribution(preprocess(text), INFERENCER_ITERATIONS, INFERENCER_THINNING,
                INFERENCER_BURN_IN);
    }

    private String getAbstract(String uri) {
        List<Triple> triples;
        String entityAbstract = null;
        triples = index.search(uri, ABSTRACT_PREDICAT, null);
        // triples = abstractIndex.search(uri);
        if ((triples != null) && (triples.size() > 0)) {
            for (Triple t : triples) {
                if ("http://dbpedia.org/ontology/abstract".equals(t.getPredicate())) {
                    entityAbstract = t.getObject();
                }
            }
        }
        return entityAbstract;
    }

    private Instance preprocess(String text) {
        com.unister.semweb.topicmodeling.utils.doc.Document tempDoc = new com.unister.semweb.topicmodeling.utils.doc.Document();
        tempDoc.addProperty(new com.unister.semweb.topicmodeling.utils.doc.DocumentText(text));
        preprocessor.getCorpus(tempDoc);

        // create an instance using the pipe of the training data
        text = tempDoc.getProperty(com.unister.semweb.topicmodeling.utils.doc.DocumentText.class).getText();
        Instance instance = new Instance(text, "NED", "hash" + text.hashCode(), null);
        instance = pipe.instanceFrom(instance);
        return instance;
    }

    protected static double getVectorLength(double[] vector) {
        double sum = 0;
        for (int i = 0; i < vector.length; i++) {
            sum += vector[i] * vector[i];
        }
        return Math.sqrt(sum);
    }

    protected static double getSimilarity(double[] topicVector, double topicVectorLength, double[] entityVector,
            double entityVectorLength) {
        double sum = 0;
        for (int i = 0; i < topicVector.length; i++) {
            sum += topicVector[i] * entityVector[i];
        }
        return sum / (topicVectorLength * entityVectorLength);
    }
}
