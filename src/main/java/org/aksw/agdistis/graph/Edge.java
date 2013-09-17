package org.aksw.agdistis.graph;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class Edge {
	private String edge;
	private Node first;
	private Node second;

	public Edge(String edge, Node first, Node second) {
		this.edge = edge;
		this.first = first;
		this.second = second;
	}

	public Pair<Node, Node> getEndpoints() {
		return new MutablePair<Node, Node>(first, second);
	}

	public Node getLeft() {
		return first;
	}

	public Node getRight() {
		return second;
	}
}
