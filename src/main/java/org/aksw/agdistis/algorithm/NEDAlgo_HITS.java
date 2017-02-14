package org.aksw.agdistis.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.graph.BreadthFirstSearch;
import org.aksw.agdistis.graph.HITS;
import org.aksw.agdistis.graph.Node;
import org.aksw.agdistis.graph.PageRank;
import org.aksw.agdistis.util.TripleIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import java.util.List;
import java.util.Map;
import org.aksw.agdistis.model.CandidatesScore;

public class NEDAlgo_HITS {

    private Logger log = LoggerFactory.getLogger(NEDAlgo_HITS.class);
    private String edgeType;
    private String nodeType;
    //private CandidateBKP cu;
    private CandidateUtil cu;
    private TripleIndex index;
    // needed for the experiment about which properties increase accuracy
    private double threshholdTrigram;
    private int maxDepth;
    private Boolean heuristicExpansionOn;
    private String algorithm;

    public NEDAlgo_HITS() throws IOException {
        Properties prop = new Properties();
        InputStream input = NEDAlgo_HITS.class.getResourceAsStream("/config/agdistis.properties");
        prop.load(input);

        String nodeType = prop.getProperty("nodeType");
        String edgeType = prop.getProperty("edgeType");
        double threshholdTrigram = Double.valueOf(prop.getProperty("threshholdTrigram"));
        int maxDepth = Integer.valueOf(prop.getProperty("maxDepth"));
        this.heuristicExpansionOn = Boolean.valueOf(prop.getProperty("heuristicExpansionOn"));
        this.algorithm = prop.getProperty("algorithm");

        this.nodeType = nodeType;
        this.edgeType = edgeType;
        this.threshholdTrigram = threshholdTrigram;
        this.maxDepth = maxDepth;

        //this.cu = new CandidateBKP();
        this.cu = new CandidateUtil();
        this.index = cu.getIndex();
    }

    public void run(Document document, Map<NamedEntityInText, List<CandidatesScore>> candidatesPerNE) {
        try {
            NamedEntitiesInText namedEntities = document.getNamedEntitiesInText();
            DirectedSparseGraph<Node, String> graph = new DirectedSparseGraph<Node, String>();

            // 0) insert candidates into Text
            log.debug("\tinsert candidates");
            cu.insertCandidatesIntoText(graph, document, threshholdTrigram, heuristicExpansionOn);

            // 1) let spread activation/ breadth first search run
            log.info("\tGraph size before BFS: " + graph.getVertexCount());
            BreadthFirstSearch bfs = new BreadthFirstSearch(index);
            bfs.run(maxDepth, graph, edgeType, nodeType);
            log.info("\tGraph size after BFS: " + graph.getVertexCount());

            if (algorithm.equals("hits")) {
                // 2.1) let HITS run
                log.info("\trun HITS");
                HITS h = new HITS();
                h.runHits(graph, 20);
            } else if (algorithm.equals("pagerank")) {
                // 2.2) let Pagerank run
                log.info("\trun PageRank");
                PageRank pr = new PageRank();
                pr.runPr(graph, 50, 0.1);
            }

            // 3) store the candidate with the highest hub, highest authority ratio
            // manipulate which value to use directly in node.compareTo
            log.debug("\torder results");
            ArrayList<Node> orderedList = new ArrayList<Node>();
            orderedList.addAll(graph.getVertices());
            Collections.sort(orderedList);
            for (NamedEntityInText entity : namedEntities) {
                for (int i = 0; i < orderedList.size(); i++) {
                    Node m = orderedList.get(i);
                    // there can be one node (candidate) for two labels
                    if (m.containsId(entity.getStartPos())) {
                        entity.setNamedEntity(m.getCandidateURI());
                        break;
                    }

                }
            }
            //To get all candidates along with their scores
            if (candidatesPerNE != null) {
                List<CandidatesScore> listCandidates = new ArrayList<>();
                for (NamedEntityInText entity : namedEntities) {
                    for (int i = 0; i < orderedList.size(); i++) {
                        Node m = orderedList.get(i);

                        // there can be one node (candidate) for two labels
                        if (m.containsId(entity.getStartPos())) {

                            CandidatesScore candidates = new CandidatesScore();
                            candidates.setStart(entity.getStartPos());
                            candidates.setUri(m.getCandidateURI());
                            candidates.setScore(m.getAuthorityWeight());
                            listCandidates.add(candidates);
                        }

                    }
                    candidatesPerNE.put(entity, listCandidates);
                }
            }

        } catch (Exception e) {
            log.error("AGDISTIS cannot be run on this document.", e);
        }
    }

    public void close() throws IOException {
        cu.close();
    }

    public void setThreshholdTrigram(double threshholdTrigram) {
        this.threshholdTrigram = threshholdTrigram;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public void setHeuristicExpansionOn(Boolean value) {
        this.heuristicExpansionOn = value;
    }

}
