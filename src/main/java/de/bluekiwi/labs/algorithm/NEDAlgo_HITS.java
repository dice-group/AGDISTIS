package de.bluekiwi.labs.algorithm;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unister.semweb.topicmodeling.utils.doc.Document;
import com.unister.semweb.topicmodeling.utils.doc.ner.NamedEntitiesInText;
import com.unister.semweb.topicmodeling.utils.doc.ner.NamedEntityInText;

import de.bluekiwi.labs.graph.BreadthFirstSearch;
import de.bluekiwi.labs.graph.HITS;
import de.bluekiwi.labs.util.SubjectPredicateObjectIndex;
import de.bluekiwi.labs.vis.MyNode;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.util.Pair;

public class NEDAlgo_HITS {
    private Logger log = LoggerFactory.getLogger(NEDAlgo_HITS.class);
    private HashMap<Integer, String> algorithmicResult = new HashMap<Integer, String>();
    private String edgeType = "http://dbpedia.org/ontology/";
    // TODO language dependend
    private String nodeType = "http://de.dbpedia.org/resource/";
    private CandidateUtil cu;

    public CandidateUtil getCu() {
        return cu;
    }

    private SubjectPredicateObjectIndex index;
    private DirectedSparseGraph<MyNode, String>[] graph = null;
    private HashSet<String> restrictedEdges;

    public NEDAlgo_HITS(int numberOfDocuments) {
        cu = new CandidateUtil();
        index = cu.getIndex();
        graph = new DirectedSparseGraph[numberOfDocuments];
    }

    public void runPreStep(Document document, double threshholdTrigram, int documentId) {
        if (graph[documentId] == null) {
            graph[documentId] = new DirectedSparseGraph<MyNode, String>();
            try {
                // 0) insert candidates into Text
                cu.insertCandidatesIntoText(graph[documentId], document, threshholdTrigram);
                // 1) let spread activation/ breadth first searc run
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
            DirectedSparseGraph<MyNode, String> tmp = clone(graph[documentId]);
            h.runHits(tmp, 20);
            log.info("DocumentId: " + documentId + " numberOfNodes: " + graph[documentId].getVertexCount() + " reduced to " + tmp.getVertexCount());
            log.info("DocumentId: " + documentId + " numberOfEdges: " + graph[documentId].getEdgeCount() + " reduced to " + tmp.getEdgeCount());
            // 3) store the candidate with the highest hub, highest authority
            // ratio
            ArrayList<MyNode> orderedList = new ArrayList<MyNode>();
            orderedList.addAll(tmp.getVertices());
            Collections.sort(orderedList);
            for (NamedEntityInText entity : namedEntities) {
                for (int i = 0; i < orderedList.size(); i++) {
                    MyNode m = orderedList.get(i);
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

    private DirectedSparseGraph<MyNode, String> clone(DirectedSparseGraph<MyNode, String> orig) {
        DirectedSparseGraph<MyNode, String> tmp = new DirectedSparseGraph<MyNode, String>();
        for (MyNode n : orig.getVertices()) {
            MyNode myNode = new MyNode(n.getCandidateURI(), n.getActivation(), n.getLevel());
            for (Integer i : n.getLabels()) {
                myNode.addId(i);
            }
            tmp.addVertex(myNode);
        }
        for (String edge : orig.getEdges()) {
            Pair<MyNode> endpoints = orig.getEndpoints(edge);
            MyNode first = endpoints.getFirst();
            MyNode second = endpoints.getSecond();
            for (MyNode nn : tmp.getVertices()) {
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

    public void run(Document document, double threshholdTrigram, int maxDepth) {
        NamedEntitiesInText namedEntities = document.getProperty(NamedEntitiesInText.class);
        algorithmicResult = new HashMap<Integer, String>();
        DirectedSparseGraph<MyNode, String> graph = new DirectedSparseGraph<MyNode, String>();
        try {
            // 0) insert candidates into Text
            cu.insertCandidatesIntoText(graph, document, threshholdTrigram);
            // 1) let spread activation/ breadth first searc run
            BreadthFirstSearch bfs = new BreadthFirstSearch(index);
            bfs.run(maxDepth, graph, edgeType, nodeType);

            // double lambda = 0.2;
            // double spreadActivationThreshold = 0.01;
            // SpreadActivation sa = new SpreadActivation();
            // sa.run(spreadActivationThreshold, maxDepth, lambda, graph);

            // 2) let HITS run
            HITS h = new HITS();
            h.runHits(graph, 20);

            // 3) store the candidate with the highest hub, highest authority
            // ratio
            ArrayList<MyNode> orderedList = new ArrayList<MyNode>();
            orderedList.addAll(graph.getVertices());
            Collections.sort(orderedList);
            for (NamedEntityInText entity : namedEntities) {
                for (int i = 0; i < orderedList.size(); i++) {
                    MyNode m = orderedList.get(i);
                    // there can be one node (candidate) for two labels
                    if (m.containsId(entity.getStartPos())) {
                        if (!algorithmicResult
                                .containsKey(entity.getStartPos())) {
                            algorithmicResult.put(entity.getStartPos(),
                                    m.getCandidateURI());
                            break;
                        }
                        // double sumAuth1Step = 0;
                        // double sumHub1Step = 0;
                        // for (MyNode suc : graph.getSuccessors(m))
                        // {
                        // sumAuth1Step += suc.getAuthorityWeight();
                        // sumHub1Step += suc.getHubWeight();
                        // }
                        // log.info("\tSumAuthorityWeightOneStepAway: " +
                        // sumAuth1Step +
                        // " \tSumHubWeightsOneStepAway: "
                        // + sumHub1Step);
                    }

                }
            }

            // if (text.getId() == 81) {
            // SimpleGraphViewWithJung sgv = new SimpleGraphViewWithJung();
            // sgv.showGraph(graph);
            // Thread.sleep(100000);
            // sgv.close();
            // }
        } catch (RepositoryException e) {
            log.error(e.getLocalizedMessage());
        } catch (InterruptedException e) {
            log.error(e.getLocalizedMessage());
        } catch (UnsupportedEncodingException e) {
            log.error(e.getLocalizedMessage());
        }
    }

    public String findResult(NamedEntityInText namedEntity) {
        if (algorithmicResult.containsKey(namedEntity.getStartPos())) {
            log.debug("\t result  " + algorithmicResult.get(namedEntity.getStartPos()));
            return algorithmicResult.get(namedEntity.getStartPos());
        } else {
            log.debug("\t result null means that we have no candidate for this NE");
            return null;
        }
    }

    public void close() {
        cu.close();
    }

    public void restrictEdgesTo(HashSet<String> restrictedEdges) {
        this.restrictedEdges = restrictedEdges;
    }

    public DirectedSparseGraph<MyNode, String>[] getAllGraphs() {
        return graph;
    }

}
