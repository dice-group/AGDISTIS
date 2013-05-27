package de.bluekiwi.labs.graph;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.openrdf.repository.RepositoryException;

import de.bluekiwi.labs.util.DBPedia;
import de.bluekiwi.labs.vis.MyNode;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class SpreadActivation {
    private static HashMap<String, MyNode> findNode = new HashMap<String, MyNode>();
    private static DBPedia dbpedia;

    public SpreadActivation() throws RepositoryException
    {
        String publicEndpoint = "http://localhost:8080/bigdata-dbpedia/sparql";
        dbpedia = new DBPedia(publicEndpoint);
    }

    public void run(double spreadActivationThreshold, int maxDepth,
            double lambda, DirectedSparseGraph<MyNode, Integer> graph) throws UnsupportedEncodingException {
        // calculate initial spreading value
        for (MyNode node : graph.getVertices()) {
            String nodeURI = node.getCandidateURI();
            double init = (nodePrestige(node) / graph.getVertexCount()) * proximity(node, nodeURI);
            node.setActivation(init);
        }

        // initial node set where spreading is done
        Queue<MyNode> q = new LinkedList<MyNode>();
        for (MyNode node : graph.getVertices()) {
            findNode.put(node.getCandidateURI(), node);
            q.add(node);
        }
        while (!q.isEmpty()) {
            /*
             * delete a node from queue for each node with an spread activation value above 0.0 spread to all outgoing
             * edges
             */
            MyNode currentNode = q.poll();
            int level = currentNode.getLevel();
            Double spreadActivationValue = currentNode.getActivation();
            if (spreadActivationValue > spreadActivationThreshold && level < maxDepth) {
                Set<String> outgoingNodes = dbpedia.getOutgoingNodes(currentNode.getCandidateURI());
                for (String targetNode : outgoingNodes) {
                    if (targetNode.contains("http://dbpedia.org/resource")) {
                        int levelNow = level + 1;
                        double activationSpread = spreadActivationValue * lambda * degree("");
                        targetNode = java.net.URLDecoder.decode(targetNode, "UTF-8");
                        MyNode myNode = null;
                        if (findNode.containsKey(targetNode))
                        {
                            myNode = findNode.get(targetNode);
                            // check if the new spread activation value is bigger than the current
                            if (myNode.getActivation() < activationSpread) {
                                myNode.setActivation(activationSpread);
                                q.add(myNode);
                            }
                        } else {
                            myNode = new MyNode(targetNode, activationSpread, levelNow);
                            findNode.put(targetNode, myNode);
                            q.add(myNode);
                        }

                        graph.addEdge(graph.getEdgeCount(), currentNode, myNode);
                    }
                }
            }
        }
    }

    /**
     * usable for weighting certain edges
     */
    private double degree(String edgePredicate) {
        return 1.0;
    }

    /**
     * can be used to prefer certain kinds of nodes for example cities
     */
    private double nodePrestige(MyNode node) {
        return 1.0;
    }

    /**
     * can be used to do something linguistic, by now there is no surface form attached to a node
     */
    private double proximity(MyNode node, String nodeURI) {
        return 1.0;
    }
}
