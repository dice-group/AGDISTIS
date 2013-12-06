package org.aksw.agdistis.algorithm;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;

import org.aksw.agdistis.graph.BreadthFirstSearch;
import org.aksw.agdistis.graph.HITS;
import org.aksw.agdistis.graph.Node;
import org.aksw.agdistis.util.TripleIndex;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.mallet.pipe.Pipe;
import cc.mallet.topics.LongBasedTopicInferencer;
import cc.mallet.types.Alphabet;

import com.unister.semweb.topicmodeling.io.StorageHelper;
import com.unister.semweb.topicmodeling.lang.Language;
import com.unister.semweb.topicmodeling.lang.postagging.PosTaggerFactory;
import com.unister.semweb.topicmodeling.lang.postagging.PosTaggingTermFilter;
import com.unister.semweb.topicmodeling.preprocessing.SingleDocumentPreprocessor;
import com.unister.semweb.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import com.unister.semweb.topicmodeling.preprocessing.docsupplier.decorator.PosTaggingSupplierDecorator;
import com.unister.semweb.topicmodeling.preprocessing.docsupplier.decorator.StemmedTextCreatorSupplierDecorator;

import datatypeshelper.utils.doc.Document;
import datatypeshelper.utils.doc.ner.NamedEntitiesInText;
import datatypeshelper.utils.doc.ner.NamedEntityInText;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.util.Pair;

public class NEDAlgo_selectedBasedCandidates_HITS implements DisambiguationAlgorithm {

    private static Logger log = LoggerFactory.getLogger(NEDAlgo_selectedBasedCandidates_HITS.class);

    private HashMap<Integer, String> algorithmicResult = new HashMap<Integer, String>();
    private String edgeType = null;
    private String nodeType = null;
    private LDABasedFilteringCandidateUtil cu = null;
    private TripleIndex index = null;
    private DirectedSparseGraph<Node, String>[] graph = null;
    // needed for the experiment about which properties increase accuracy
    private HashSet<String> restrictedEdges = null;
    private double threshholdTrigram = 0.9;
    private int maxDepth = 2;

    public static NEDAlgo_selectedBasedCandidates_HITS createAlgorithm(File indexDirectory, File inferencerFile,
            File pipeFile, String nodeType, String edgeType) {
        // LDA model --> inferencer
        LongBasedTopicInferencer inferencer;
        try {
            if (inferencerFile.getName().endsWith(".gz")) {
                inferencer = LongBasedTopicInferencer.readFromModelStateFile(inferencerFile);
            } else {
                inferencer = LongBasedTopicInferencer.read(inferencerFile);
            }
        } catch (Exception e) {
            log.error("Couldn't load topic model inferencer. Returning null.", e);
            return null;
        }

        Pipe pipe = StorageHelper.readFromFileSavely(pipeFile.getAbsolutePath());
        if (pipe == null) {
            log.error("Couldn't load the mallet preprocessing pipe from file. Returning null.");
            return null;
        }

        final Alphabet alphabet = inferencer.getAlphabet();

        // create Preprocessing
        SingleDocumentPreprocessor preprocessor = new SingleDocumentPreprocessor();
        DocumentSupplier supplier = preprocessor;
        supplier = new PosTaggingSupplierDecorator(supplier, PosTaggerFactory.getPosTaggingStep(Language.ENG,
                new PosTaggingTermFilter() {
                    private Pattern numberPattern = Pattern.compile(".*\\d.*");

                    @Override
                    public boolean isTermGood(com.unister.semweb.ml.text.features.Term term) {
                        // return vocabulary.getId(term.getLemma()) != Vocabulary.WORD_NOT_FOUND;
                        // XXX ADDED CONDITIONS FOR DEBUGGING!
                        String lemma = term.getLemma();
                        return alphabet.contains(lemma) && (lemma.length() > 2)
                                && (!numberPattern.matcher(lemma).matches());
                    }
                }));
        supplier = new StemmedTextCreatorSupplierDecorator(supplier);
        preprocessor.setDocumentSupplier(supplier);

        LDABasedFilteringCandidateUtil cu = new LDABasedFilteringCandidateUtil(indexDirectory, preprocessor,
                inferencer, pipe);

        return new NEDAlgo_selectedBasedCandidates_HITS(indexDirectory, cu, nodeType, edgeType);
    }

    protected NEDAlgo_selectedBasedCandidates_HITS(File indexDirectory, LDABasedFilteringCandidateUtil cu,
            String nodeType, String edgeType) {
        this.nodeType = nodeType;
        this.edgeType = edgeType;
        this.cu = cu;
        this.index = cu.getIndex();
        this.graph = new DirectedSparseGraph[1];
    }

    public void runPreStep(Document document, double threshholdTrigram, int documentId) {
        if (graph[documentId] == null) {
            graph[documentId] = new DirectedSparseGraph<Node, String>();
            try {
                // 0) insert candidates into Text
                cu.insertCandidatesIntoText(graph[documentId], document, threshholdTrigram);
                // 1) let spread activation/ breadth first search run
                int maxDepth = 2;
                BreadthFirstSearch bfs = new BreadthFirstSearch(index);
                bfs.run(maxDepth, graph[documentId], edgeType, nodeType);
            } catch (RepositoryException e) {
                log.error(e.getLocalizedMessage());
            } catch (UnsupportedEncodingException e) {
                log.error(e.getLocalizedMessage());
            }
        }
    }

    public void runPostStep(Document document, double threshholdTrigram, int documentId) {
        try {
            algorithmicResult = new HashMap<Integer, String>();
            NamedEntitiesInText namedEntities = document.getProperty(NamedEntitiesInText.class);
            // 2) let HITS run
            HITS h = new HITS();
            h.restrictEdges(restrictedEdges);
            // take a copied graph
            DirectedSparseGraph<Node, String> tmp = clone(graph[documentId]);
            h.runHits(tmp, 20);
            log.info("DocumentId: " + documentId + " numberOfNodes: " + graph[documentId].getVertexCount()
                    + " reduced to " + tmp.getVertexCount());
            log.info("DocumentId: " + documentId + " numberOfEdges: " + graph[documentId].getEdgeCount()
                    + " reduced to " + tmp.getEdgeCount());
            // 3) store the candidate with the highest hub, highest authority
            // ratio
            ArrayList<Node> orderedList = new ArrayList<Node>();
            orderedList.addAll(tmp.getVertices());
            Collections.sort(orderedList);
            for (NamedEntityInText entity : namedEntities) {
                for (int i = 0; i < orderedList.size(); i++) {
                    Node m = orderedList.get(i);
                    // there can be one node (candidate) for two labels
                    if (m.containsId(entity.getStartPos())) {
                        if (!algorithmicResult.containsKey(entity.getStartPos())) {
                            algorithmicResult.put(entity.getStartPos(), m.getCandidateURI());
                            break;
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private DirectedSparseGraph<Node, String> clone(DirectedSparseGraph<Node, String> orig) {
        DirectedSparseGraph<Node, String> tmp = new DirectedSparseGraph<Node, String>();
        for (Node n : orig.getVertices()) {
            Node Node = new Node(n.getCandidateURI(), n.getActivation(), n.getLevel());
            for (Integer i : n.getLabels()) {
                Node.addId(i);
            }
            tmp.addVertex(Node);
        }
        for (String edge : orig.getEdges()) {
            Pair<Node> endpoints = orig.getEndpoints(edge);
            Node first = endpoints.getFirst();
            Node second = endpoints.getSecond();
            for (Node nn : tmp.getVertices()) {
                if (nn.getCandidateURI().equals(first.getCandidateURI())) {
                    first = nn;
                }
                if (nn.getCandidateURI().equals(second.getCandidateURI())) {
                    second = nn;
                }
            }
            tmp.addEdge(edge, first, second);
        }
        return tmp;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.aksw.agdistis.algorithm.DisambiguationAlgorithm#run(datatypeshelper
     * .utils.doc.Document, double, int)
     */
    @Override
    public void run(Document document) {
        NamedEntitiesInText namedEntities = document.getProperty(NamedEntitiesInText.class);
        algorithmicResult = new HashMap<Integer, String>();
        DirectedSparseGraph<Node, String> graph = new DirectedSparseGraph<Node, String>();

        try {
            // 0) insert candidates into Text
            log.debug("\tinsert candidates");
            cu.insertCandidatesIntoText(graph, document, threshholdTrigram);

            // 1) let spread activation/ breadth first searc run
            log.info("\tGraph size before BFS: " + graph.getVertexCount());
            BreadthFirstSearch bfs = new BreadthFirstSearch(index);
            bfs.run(maxDepth, graph, edgeType, nodeType);
            log.info("\tGraph size after BFS: " + graph.getVertexCount());
            // double lambda = 0.2;
            // double spreadActivationThreshold = 0.01;
            // SpreadActivation sa = new SpreadActivation();
            // sa.run(spreadActivationThreshold, maxDepth, lambda, graph);

            // 2) let HITS run
            log.debug("\trun HITS");
            HITS h = new HITS();
            h.runHits(graph, 20);

            // 3) store the candidate with the highest hub, highest authority
            // ratio
            log.debug("\torder results");
            ArrayList<Node> orderedList = new ArrayList<Node>();
            orderedList.addAll(graph.getVertices());
            Collections.sort(orderedList);
            for (NamedEntityInText entity : namedEntities) {
                for (int i = 0; i < orderedList.size(); i++) {
                    Node m = orderedList.get(i);
                    // there can be one node (candidate) for two labels
                    if (m.containsId(entity.getStartPos())) {
                        if (!algorithmicResult.containsKey(entity.getStartPos())) {
                            algorithmicResult.put(entity.getStartPos(), m.getCandidateURI());
                            break;
                        }
                    }

                }
            }

        } catch (RepositoryException e) {
            log.error(e.getLocalizedMessage());
        } catch (InterruptedException e) {
            log.error(e.getLocalizedMessage());
        } catch (UnsupportedEncodingException e) {
            log.error(e.getLocalizedMessage());
        }
    }

    @Override
    public String findResult(NamedEntityInText namedEntity) {
        if (algorithmicResult.containsKey(namedEntity.getStartPos())) {
            log.debug("\t result  " + algorithmicResult.get(namedEntity.getStartPos()));
            return algorithmicResult.get(namedEntity.getStartPos());
        } else {
            log.debug("\t result null means that we have no candidate for this NE");
            return null;
        }
    }

    @Override
    public void close() {
        cu.close();
    }

    public void restrictEdgesTo(HashSet<String> restrictedEdges) {
        this.restrictedEdges = restrictedEdges;
    }

    public DirectedSparseGraph<Node, String>[] getAllGraphs() {
        return graph;
    }

    public void setThreshholdTrigram(double threshholdTrigram) {
        this.threshholdTrigram = threshholdTrigram;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    @Override
    public String getRedirect(String findResult) {
        return cu.redirect(findResult);
    }

    @Override
    public double getThreshholdTrigram() {
        return this.threshholdTrigram;
    }
}