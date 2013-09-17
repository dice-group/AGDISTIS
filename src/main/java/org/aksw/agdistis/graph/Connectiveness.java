package org.aksw.agdistis.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import edu.uci.ics.jung.graph.DirectedSparseGraph;

/**
 * not used for final version of the paper
 * 
 * @author r.usbeck
 * 
 */
public class Connectiveness {
    public double meassureConnectiveness(DirectedSparseGraph<Node, Integer> graph) {
        /*
         * check whether every initial node is reachable from every other node because it is an undirected graph it
         * suffice to check whether one node can reach all other nodes
         */
        ArrayList<Node> list = new ArrayList<Node>();
        for (Node tmp : graph.getVertices()) {
            if (tmp.getLevel() == 0) {
                list.add(tmp);
            }
        }
        // measure connectedness on a directed graph
        double numberOfConnections = 0;
        for (Node fromNode : list) {
            for (Node toNode : list) {
                if (toNode != fromNode) {
                    // test whether there is a path from fromNode to toNode
                    Queue<Node> queueBFS = new LinkedList<Node>();
                    HashSet<Node> alreadySeenNodes = new HashSet<Node>();
                    queueBFS.add(fromNode);
                    alreadySeenNodes.add(fromNode);
                    while (!queueBFS.isEmpty()) {
                        // could be a problem if there is a cycle
                        Node currentNode = queueBFS.poll();
                        Collection<Node> incidentNodes = new ArrayList<Node>();
                        for (int incEdge : graph.getIncidentEdges(currentNode)) {
                            for (Node incNodes : graph.getIncidentVertices(incEdge)) {
                                if (!incNodes.getCandidateURI().equals(currentNode.getCandidateURI())) {
                                    incidentNodes.add(incNodes);
                                }
                            }
                        }
                        if (incidentNodes.contains(toNode)) {
                            // found a path to toNode via BFS
                            numberOfConnections += 1.0;
                            break;
                        } else {
                            // put those nodes to the queue that haven't been seen
                            for (Node Node : incidentNodes) {
                                if (!alreadySeenNodes.contains(Node)) {
                                    queueBFS.add(Node);
                                    alreadySeenNodes.add(Node);
                                }
                            }
                        }
                    }
                }
            }
        }
        // multiply by 2 because of the running over list array twice
        double fractionOfConnectedNodes = numberOfConnections / ((list.size()) * ((double) list.size() - 1));
        return fractionOfConnectedNodes;
    }
}
