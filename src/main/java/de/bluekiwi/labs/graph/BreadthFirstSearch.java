package de.bluekiwi.labs.graph;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.openrdf.repository.RepositoryException;

import de.bluekiwi.labs.util.SubjectPredicateObjectIndex;
import de.bluekiwi.labs.util.Triple;
import de.bluekiwi.labs.vis.MyNode;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class BreadthFirstSearch {
    private static HashMap<String, MyNode> findNode = new HashMap<String, MyNode>();
    private SubjectPredicateObjectIndex index;

    public BreadthFirstSearch(SubjectPredicateObjectIndex index) throws RepositoryException
    {
        this.index = index;
    }

    public void run(int maxDepth, DirectedSparseGraph<MyNode, String> graph, String edgeType, String nodeType)
            throws UnsupportedEncodingException {
        Queue<MyNode> q = new LinkedList<MyNode>();
        for (MyNode node : graph.getVertices()) {
            findNode.put(node.getCandidateURI(), node);
            q.add(node);
        }
        while (!q.isEmpty()) {
            MyNode currentNode = q.poll();
            int level = currentNode.getLevel();
            if (level < maxDepth) {
                List<Triple> outgoingNodes = index.search(currentNode.getCandidateURI());
                for (Triple targetNode : outgoingNodes) {
                    if (targetNode.getPredicate().startsWith(edgeType) && targetNode.getObject().startsWith(nodeType)) {
                        int levelNow = level + 1;
                        MyNode myNode = null;
                        if (findNode.containsKey(targetNode)) {
                            myNode = findNode.get(targetNode);
                        } else {
                            myNode = new MyNode(targetNode.getObject(), 0, levelNow);
                            findNode.put(targetNode.getObject(), myNode);
                            q.add(myNode);
                        }
                        graph.addEdge(graph.getEdgeCount() + ";" + targetNode.getPredicate(), currentNode, myNode);
                    }
                }
            }
        }
    }
}
