package org.aksw.agdistis.algorithm;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;

import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.graph.BreadthFirstSearch;
import org.aksw.agdistis.graph.HITS;
import org.aksw.agdistis.graph.Node;
import org.aksw.agdistis.util.TripleIndex;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class NEDAlgo_HITS {

	private Logger log = LoggerFactory.getLogger(NEDAlgo_HITS.class);
	private HashMap<Integer, String> algorithmicResult = new HashMap<Integer, String>();
	private String edgeType;
	private String nodeType;
	private CandidateUtil cu;
	private TripleIndex index;
	// needed for the experiment about which properties increase accuracy
	private double threshholdTrigram;
	private int maxDepth;

	public NEDAlgo_HITS() throws IOException {
			Properties prop = new Properties();
			InputStream input = new FileInputStream("agdistis.properties");
			prop.load(input);

			String nodeType = prop.getProperty("nodeType");
			String edgeType = prop.getProperty("edgeType");
			double threshholdTrigram = Double.valueOf(prop.getProperty("threshholdTrigram"));
			int maxDepth = Integer.valueOf(prop.getProperty("maxDepth"));

			this.nodeType = nodeType;
			this.edgeType = edgeType;
			this.threshholdTrigram = threshholdTrigram;
			this.maxDepth = maxDepth;

			this.cu = new CandidateUtil();
			this.index = cu.getIndex();
	}

	public void run(Document document) {
		try {
			NamedEntitiesInText namedEntities = document.getNamedEntitiesInText();
			algorithmicResult = new HashMap<Integer, String>();
			DirectedSparseGraph<Node, String> graph = new DirectedSparseGraph<Node, String>();

			// 0) insert candidates into Text
			log.debug("\tinsert candidates");
			cu.insertCandidatesIntoText(graph, document, threshholdTrigram);

			// 1) let spread activation/ breadth first search run
			log.info("\tGraph size before BFS: " + graph.getVertexCount());
			BreadthFirstSearch bfs = new BreadthFirstSearch(index);
			bfs.run(maxDepth, graph, edgeType, nodeType);
			log.info("\tGraph size after BFS: " + graph.getVertexCount());

			// 2) let HITS run
			log.debug("\trun HITS");
			HITS h = new HITS();
			h.runHits(graph, 20);

			// 3) store the candidate with the highest hub, highest authority ratio
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

		} catch (RepositoryException | IOException | InterruptedException e) {
			log.error("AGDISTIS cannot be run on this document.", e);
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

}