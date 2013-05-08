package de.bluekiwi.labs.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import de.bluekiwi.labs.vis.MyNode;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class Connectiveness {
    public double meassureConnectiveness(DirectedSparseGraph<MyNode, Integer> graph) {
        /*
         * check whether every initial node is reachable from every other node because it is an undirected graph it
         * suffice to check whether one node can reach all other nodes
         */
        ArrayList<MyNode> list = new ArrayList<MyNode>();
        for (MyNode tmp : graph.getVertices()) {
            if (tmp.getLevel() == 0) {
                list.add(tmp);
            }
        }
        // meassure connectedness on a directed graph
        double numberOfConnections = 0;
        for (MyNode fromNode : list) {
            for (MyNode toNode : list) {
                if (toNode != fromNode) {
                    // test whether there is a path from fromNode to toNode
                    Queue<MyNode> queueBFS = new LinkedList<MyNode>();
                    HashSet<MyNode> alreadySeenNodes = new HashSet<MyNode>();
                    queueBFS.add(fromNode);
                    alreadySeenNodes.add(fromNode);
                    while (!queueBFS.isEmpty()) {
                        // could be a problem if there is a cycle
                        MyNode currentNode = queueBFS.poll();
                        Collection<MyNode> incidentNodes = new ArrayList<MyNode>();
                        for (int incEdge : graph.getIncidentEdges(currentNode)) {
                            for (MyNode incNodes : graph.getIncidentVertices(incEdge)) {
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
                            for (MyNode myNode : incidentNodes) {
                                if (!alreadySeenNodes.contains(myNode)) {
                                    queueBFS.add(myNode);
                                    alreadySeenNodes.add(myNode);
                                }
                            }
                        }
                    }
                }
            }
        }
        // multiply by 2 because of the running over list array twice
        double fractionOfConnectedNodes = numberOfConnections
                / ((list.size()) * ((double) list.size() - 1));
        return fractionOfConnectedNodes;
    }
}
