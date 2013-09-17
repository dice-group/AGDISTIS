package org.aksw.agdistis.experiment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;

import org.aksw.agdistis.graph.HITS;
import org.aksw.agdistis.graph.Node;
import org.aksw.agdistis.graph.SpreadActivation;
import org.openrdf.repository.RepositoryException;
import org.slf4j.LoggerFactory;

import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class HITSValues {
    private static org.slf4j.Logger log = LoggerFactory.getLogger(HITSValues.class);

    private static String fileToGraphs = "example.graph";
    private static boolean writeFile = false;
    private static BufferedWriter bw = null;

    public static void main(String[] args) throws UnsupportedEncodingException, IOException, RepositoryException,
            InterruptedException {
        ArrayList<String> graphNames = new ArrayList<String>();
        ArrayList<DirectedSparseGraph<Node, Integer>> graphs = new ArrayList<DirectedSparseGraph<Node, Integer>>();

        BufferedReader br = new BufferedReader(new FileReader(fileToGraphs));
        while (br.ready()) {
            graphNames.add(br.readLine());
            String edges = br.readLine();
            if (!edges.matches("")) {
                DirectedSparseGraph<Node, Integer> targetGraph = new DirectedSparseGraph<Node, Integer>();
                for (String node : edges.split("\t")) {
                    node = java.net.URLDecoder.decode(node, "UTF-8");
                    Node currentNode = new Node(node, 0, 0);
                    targetGraph.addVertex(currentNode);
                }
                graphs.add(targetGraph);
            }
        }
        br.close();

        if (writeFile) {
            bw = new BufferedWriter(new FileWriter("spreadingResults.txt"));
            bw.write("graph\tmaxDepth\tlambda(detunation)\tConnectivness");
            bw.newLine();
        }

        SpreadActivation sa = new SpreadActivation();
        double spreadActivationThreshold = 0.01;
        int maxDepth = 2;
        double lambda = 0.2;
        for (int i = 0; i < graphs.size(); i++) {
            // let SpreadActivation run
            sa.run(spreadActivationThreshold, maxDepth, lambda, graphs.get(i));

            // let HITS run
            HITS h = new HITS();
            h.runHits(graphs.get(i), 20);

            ArrayList<Node> orderedList = new ArrayList<Node>();
            orderedList.addAll(graphs.get(i).getVertices());
            Collections.sort(orderedList);
            for (Node m : orderedList)
            {
                if (m.getLevel() == 0) {
                    double sumAuth1Step = 0;
                    double sumHub1Step = 0;
                    for (Node suc : graphs.get(i).getSuccessors(m))
                    {
                        sumAuth1Step += suc.getAuthorityWeight();
                        sumHub1Step += suc.getHubWeight();
                    }
                    log.info(m.toString());
                    log.info("\tAuthorityWeight: " + m.getAuthorityWeight() + " HubWeight: " + m.getHubWeight());
                    log.info("\tSumAuthorityWeightOneStepAway: " + sumAuth1Step + " \tSumHubWeightsOneStepAway: " + sumHub1Step);
                    if (writeFile) {
                        bw.write(m.getCandidateURI() + "\n");
                        bw.write("\tAuthorityWeight: " + m.getAuthorityWeight() + " HubWeight: " + m.getHubWeight());
                        bw.write("\tSumAuthorityWeightOneStepAway: " + sumAuth1Step + " \tSumHubWeightsOneStepAway: " + sumHub1Step);
                        bw.newLine();
                        bw.flush();
                    }
                }
            }
        }
        if (writeFile) {
            bw.close();
        }
    }
}
