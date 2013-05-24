package de.bluekiwi.labs.graph;

import java.util.HashSet;

import de.bluekiwi.labs.vis.MyNode;
import edu.uci.ics.jung.graph.Graph;

public class HITS {
    private HashSet<String> restrictedEdges;

    /**
     * 
     * this methods runs hits on a graph and returns the most authoritive sources, due to HITS is very simple, it will
     * be implemented by iterating two formulas can be written as matrix multiplication but this would be quite
     * ineffective
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
        // System.out.println("before: " + g.getEdgeCount());
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
        // System.out.println("after: " + g.getEdgeCount());
        for (int iter = 0; iter < k; iter++) {
            for (Object o : g.getVertices()) {
                MyNode m = (MyNode) o;
                double x = 0;
                for (Object inc : g.getPredecessors(m)) {
                    x += ((MyNode) inc).getHubWeight();
                }
                double y = 0;
                for (Object inc : g.getSuccessors(m)) {
                    y += ((MyNode) inc).getAuthorityWeight();
                }
                m.setAuthorityWeight(x);
                m.setHubWeight(y);
            }
            // build normalization
            double sumX = 0;
            double sumY = 0;
            for (Object m : g.getVertices()) {
                MyNode mm = (MyNode) m;
                sumX += mm.getAuthorityWeight();
                sumY += mm.getHubWeight();
            }
            for (Object m : g.getVertices()) {
                MyNode mm = (MyNode) m;
                mm.setAuthorityWeight(mm.getAuthorityWeight() / sumX);
                mm.setHubWeight(mm.getHubWeight() / sumY);
            }
        }
    }

    public void restrictEdges(HashSet<String> restrictedEdges) {
        this.restrictedEdges = restrictedEdges;

    }
}
