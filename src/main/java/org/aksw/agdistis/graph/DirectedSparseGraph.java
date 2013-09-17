package org.aksw.agdistis.graph;

import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.lang3.tuple.Pair;

public class DirectedSparseGraph {
	private HashSet<Node> nodes;
	private HashMap<String, Edge> edges;

	public DirectedSparseGraph() {
		nodes = new HashSet<Node>();
		edges = new HashMap<String, Edge>();

	}

	public boolean addVertex(Node node) {
		return nodes.add(node);
	}

	public HashSet<Node> getVertices() {
		return nodes;
	}

	public int getVertexCount() {
		return nodes.size();
	}

	public int getEdgeCount() {
		return edges.size();
	}

	public HashSet<Node> getSuccessors(Node node) {
		return node.getSuccessors();
	}

	public HashSet<Node> getPredecessors(Node node) {
		return node.getPredecessors();
	}

	public HashMap<String, Edge> getEdges() {
		return edges;
	}

	public Pair<Node, Node> getEndpoints(String edge) {
		Edge e = edges.get(edge);
		return e.getEndpoints();
	}

	public void addEdge(String edge, Node first, Node second) {
		Edge e = new Edge(edge, first, second);
		first.addSuccesor(second);
		second.addPredecessor(first);
	}

	public void removeEdge(Edge edge) {
		edge.getLeft().removeSuccesor(edge.getRight());
		edge.getRight().removePredecessor(edge.getLeft());
		edges.remove(edge);

	}

}
