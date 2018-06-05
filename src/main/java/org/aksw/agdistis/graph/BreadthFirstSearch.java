package org.aksw.agdistis.graph;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.aksw.agdistis.util.Triple;
import org.aksw.agdistis.util.TripleIndex;

import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class BreadthFirstSearch {
	private static HashMap<String, Node> findNode = new HashMap<String, Node>();
	private TripleIndex index;
	private String algo;

	public BreadthFirstSearch(TripleIndex index, String algo) {
		this.index = index;
		this.algo = algo;
	}
	private boolean startsWith(String url,String[]set){
		for(int i=0;i<set.length;i++)
			if(url.startsWith(set[i]))
				return true;
		return false;
	}

	public void run(int maxDepth, DirectedSparseGraph<Node, String> graph, String edgeType, String nodeType)
			throws UnsupportedEncodingException, IOException {
		Queue<Node> q = new LinkedList<Node>();
		for (Node node : graph.getVertices()) {
			findNode.put(node.getCandidateURI(), node);
			q.add(node);
		}
		while (!q.isEmpty()) {
			Node currentNode = q.poll();
			int level = currentNode.getLevel();
			if (level < maxDepth) {
				List<Triple> outgoingNodes = null;
				outgoingNodes = index.search(currentNode.getCandidateURI(), null, null);
				if (outgoingNodes == null) {
					continue;
				}
				for (Triple targetNode : outgoingNodes) {
					if (targetNode.getPredicate() == null && targetNode.getObject() == null) {
						continue;
					}
					//if (targetNode.getPredicate().startsWith(edgeType) && targetNode.getObject().startsWith(nodeType)) {
					if (startsWith(targetNode.getPredicate(),edgeType.split(",")) && startsWith(targetNode.getObject(),nodeType.split(","))) {
						int levelNow = level + 1;
						Node Node = null;
						if (findNode.containsKey(targetNode)) {
							Node = findNode.get(targetNode);
						} else {
							Node = new Node(targetNode.getObject(), 0, levelNow, algo);
							findNode.put(targetNode.getObject(), Node);
							q.add(Node);
						}
						graph.addEdge(graph.getEdgeCount() + ";" + targetNode.getPredicate(), currentNode, Node);
					}
				}
			}
		}
	}
}
