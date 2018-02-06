package org.aksw.agdistis.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.graph.BreadthFirstSearch;
import org.aksw.agdistis.graph.HITS;
import org.aksw.agdistis.graph.Node;
import org.aksw.agdistis.graph.PageRank;
import org.aksw.agdistis.model.CandidatesScore;
import org.aksw.agdistis.util.TripleIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class NEDAlgo_HITS {

  private final Logger log = LoggerFactory.getLogger(NEDAlgo_HITS.class);
  private String edgeType;
  private String nodeType;
  private final CandidateUtil cu;
  private TripleIndex index;
  // needed for the experiment about which properties increase accuracy
  private double threshholdTrigram;
  private int maxDepth;
  private Boolean heuristicExpansionOn;
  private final String algorithm;

  public NEDAlgo_HITS() throws IOException {
    final Properties prop = new Properties();
    final InputStream input = NEDAlgo_HITS.class.getResourceAsStream("/config/agdistis.properties");
    prop.load(input);

    final String envNodeType = System.getenv("AGDISTIS_NODE_TYPE");
    final String nodeType = envNodeType != null ? envNodeType : prop.getProperty("nodeType");
    final String envEdgeType = System.getenv("AGDISTIS_EDGE_TYPE");
    final String edgeType = envEdgeType != null ? envEdgeType : prop.getProperty("edgeType");
    final String envThresholdTrigram = System.getenv("AGDISTIS_THRESHHOLD_TRIGRAM");
    final double threshholdTrigram = Double.valueOf(
        envThresholdTrigram != null ? envThresholdTrigram : prop.getProperty("threshholdTrigram"));
    final String envMaxDepth = System.getenv("AGDISTIS_MAX_DEPTH");
    final int maxDepth =
        Integer.valueOf(envMaxDepth != null ? envMaxDepth : prop.getProperty("maxDepth"));
    final String envHeuristicExpansion = System.getenv("AGDISTIS_HEURISTIC_EXPANSION_ON");
    heuristicExpansionOn = Boolean.valueOf(envHeuristicExpansion != null ? envHeuristicExpansion
        : prop.getProperty("heuristicExpansionOn"));
    final String envAlgorithm = System.getenv("AGDISTIS_ALGORITHM");
    algorithm = envAlgorithm != null ? envAlgorithm : prop.getProperty("algorithm");
    this.nodeType = nodeType;
    this.edgeType = edgeType;
    this.threshholdTrigram = threshholdTrigram;
    this.maxDepth = maxDepth;
    cu = new CandidateUtil();
    index = cu.getIndex();
  }

  public void run(final Document document,
      final Map<NamedEntityInText, List<CandidatesScore>> candidatesPerNE) {
    try {
      final NamedEntitiesInText namedEntities = document.getNamedEntitiesInText();
      final DirectedSparseGraph<Node, String> graph = new DirectedSparseGraph<Node, String>();

      // 0) insert candidates into Text
      log.debug("\tinsert candidates");
      cu.insertCandidatesIntoText(graph, document, threshholdTrigram, heuristicExpansionOn);

      // 1) let spread activation/ breadth first search run
      log.info("\tGraph size before BFS: " + graph.getVertexCount());
      final BreadthFirstSearch bfs = new BreadthFirstSearch(index, algorithm);
      bfs.run(maxDepth, graph, edgeType, nodeType);
      log.info("\tGraph size after BFS: " + graph.getVertexCount());

      if (algorithm.equals("hits")) {
        // 2.1) let HITS run
        log.info("\trun HITS");
        final HITS h = new HITS();
        h.runHits(graph, 20);
      } else if (algorithm.equals("pagerank")) {
        // 2.2) let Pagerank run
        log.info("\trun PageRank");
        final PageRank pr = new PageRank();
        pr.runPr(graph, 50, 0.1);
      }

      // 3) store the candidate with the highest hub, highest authority
      // ratio
      // manipulate which value to use directly in node.compareTo
      log.debug("\torder results");
      final ArrayList<Node> orderedList = new ArrayList<Node>();
      orderedList.addAll(graph.getVertices());
      Collections.sort(orderedList);
      for (final NamedEntityInText entity : namedEntities) {
        for (int i = 0; i < orderedList.size(); i++) {
          final Node m = orderedList.get(i);
          // there can be one node (candidate) for two labels
          if (m.containsId(entity.getStartPos())) {
            entity.setNamedEntity(m.getCandidateURI());
            break;
          }

        }
      }
      // To get all candidates along with their scores
      if (candidatesPerNE != null) {
        final List<CandidatesScore> listCandidates = new ArrayList<>();
        for (final NamedEntityInText entity : namedEntities) {
          for (int i = 0; i < orderedList.size(); i++) {
            final Node m = orderedList.get(i);

            // there can be one node (candidate) for two labels
            if (m.containsId(entity.getStartPos())) {

              final CandidatesScore candidates = new CandidatesScore();
              candidates.setStart(entity.getStartPos());
              candidates.setUri(m.getCandidateURI());
              candidates.setScore(m.getAuthorityWeight());
              listCandidates.add(candidates);
            }

          }
          candidatesPerNE.put(entity, listCandidates);
        }
      }

    } catch (final Exception e) {
      log.error("AGDISTIS cannot be run on this document.", e);
    }
  }

  public void close() throws IOException {
    cu.close();
  }

  public void setThreshholdTrigram(final double threshholdTrigram) {
    this.threshholdTrigram = threshholdTrigram;
  }

  public void setMaxDepth(final int maxDepth) {
    this.maxDepth = maxDepth;
  }

  public void setHeuristicExpansionOn(final Boolean value) {
    heuristicExpansionOn = value;
  }

  public String getEdgeType() {
    return edgeType;
  }

  public void setEdgeType(final String edgeType) {
    this.edgeType = edgeType;
  }

  public String getNodeType() {
    return nodeType;
  }

  public void setNodeType(final String nodeType) {
    this.nodeType = nodeType;
    cu.setNodeType(nodeType);
  }

  public void setIndex(final TripleIndex index) {
    this.index = index;
    cu.setIndex(index);
  }

}
