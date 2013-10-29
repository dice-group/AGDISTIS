package org.aksw.agdistis.algorithm;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.aksw.agdistis.graph.BreadthFirstSearch;
import org.aksw.agdistis.graph.HITS;
import org.aksw.agdistis.graph.Node;
import org.aksw.agdistis.util.SubjectPredicateObjectIndex;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datatypeshelper.utils.doc.Document;
import datatypeshelper.utils.doc.ner.NamedEntitiesInText;
import datatypeshelper.utils.doc.ner.NamedEntityInText;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.util.Pair;

public class NEDAlgo_HITS implements DisambiguationAlgorithm {

	private Logger log = LoggerFactory.getLogger(NEDAlgo_HITS.class);
	private HashMap<Integer, String> algorithmicResult = new HashMap<Integer, String>();
	private String edgeType = null;
	private String nodeType = null;
	private CandidateUtil cu = null;
	private SubjectPredicateObjectIndex index = null;
	private DirectedSparseGraph<Node, String>[] graph = null;
	// needed for the experiment about which properties increase accuracy
	private HashSet<String> restrictedEdges = null;
	private double threshholdTrigram = 0.82;
	private int maxDepth = 2;

	@SuppressWarnings("unchecked")
	public NEDAlgo_HITS(int numberOfDocuments, String languageTag, String dataDirectory, String knowledgeBase, String edgeType) {
		this.nodeType = knowledgeBase;
		this.cu = new CandidateUtil(languageTag, dataDirectory, knowledgeBase);
		this.index = cu.getIndex();
		this.graph = new DirectedSparseGraph[numberOfDocuments];
		this.edgeType = edgeType;
	}

	public NEDAlgo_HITS(String modelDirectory, String knowledgeBase) {
		nodeType = knowledgeBase;
		cu = new CandidateUtil("en", modelDirectory, knowledgeBase);
		index = cu.getIndex();
		graph = new DirectedSparseGraph[1];
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
			log.info("DocumentId: " + documentId + " numberOfNodes: " + graph[documentId].getVertexCount() + " reduced to " + tmp.getVertexCount());
			log.info("DocumentId: " + documentId + " numberOfEdges: " + graph[documentId].getEdgeCount() + " reduced to " + tmp.getEdgeCount());
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
			log.debug("\trun BFS");

			BreadthFirstSearch bfs = new BreadthFirstSearch(index);
			bfs.run(maxDepth, graph, edgeType, nodeType);

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.aksw.agdistis.algorithm.DisambiguationAlgorithm#findResult(
	 * datatypeshelper.utils.doc.ner.NamedEntityInText)
	 */
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.aksw.agdistis.algorithm.DisambiguationAlgorithm#close()
	 */
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
}