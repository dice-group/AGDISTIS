package org.aksw.agdistis.graph;

import java.util.HashSet;

import edu.uci.ics.jung.graph.Graph;

public class HITS {
	private HashSet<String> restrictedEdges;

	/**
	 * 
	 * this methods runs hits on a graph and returns the most authoritative sources, due to HITS is very simple, it will be implemented by iterating two formulas can be written as matrix multiplication but this would be quite ineffective
	 * 
	 * @param k
	 * 
	 * @param Graph
	 * @return
	 * @throws InterruptedException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void runHits(Graph g, int k) throws InterruptedException {
		// restrict Graph
		if (restrictedEdges != null) {
			HashSet<Object> toBeRemoved = new HashSet<Object>();
			for (Object edge : g.getEdges()) {
				String edgeString = (String) edge;
				for (String restrict : restrictedEdges) {
					if (edgeString.contains(restrict)) {
						toBeRemoved.add(edge);
					}
				}
			}
			// because of concurrent modification exception
			for (Object edge : toBeRemoved) {
				g.removeEdge(edge);
			}
		}
		// x - authority weight
		// y - hub weight
		Node n;
		for (int iter = 0; iter < k; iter++) {
			for (Object o : g.getVertices()) {
				n = (Node) o;
				double x = 0;
				for (Object inc : g.getPredecessors(n)) {
					x += ((Node) inc).getHubWeight();
				}
				double y = 0;
				for (Object inc : g.getSuccessors(n)) {
					y += ((Node) inc).getAuthorityWeight();
				}
				n.setUnnormalizedAuthorityWeight(x * n.getAuthorityWeightForCalculation());
				n.setUnnormalizedHubWeight(y * n.getHubWeightForCalculation());
			}
			// build normalization
			double sumX = 0;
			double sumY = 0;
			for (Object o : g.getVertices()) {
				n = (Node) o;
				sumX += n.getUnnormalizedAuthorityWeight();
				sumY += n.getUnnormalizedHubWeight();
			}
			for (Object o : g.getVertices()) {
				n = (Node) o;
				n.setAuthorityWeight(n.getUnnormalizedAuthorityWeight() / sumX);
				n.setHubWeight(n.getUnnormalizedHubWeight() / sumY);
			}
		}
	}

	public void restrictEdges(HashSet<String> restrictedEdges) {
		this.restrictedEdges = restrictedEdges;

	}
}
