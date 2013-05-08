package de.bluekiwi.labs.experiment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.openrdf.repository.RepositoryException;

import de.bluekiwi.labs.graph.Connectiveness;
import de.bluekiwi.labs.graph.SpreadActivation;
import de.bluekiwi.labs.vis.MyNode;
import de.bluekiwi.labs.vis.SimpleGraphViewWithJung;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class SpreadDepth {
    private static String fileToGraphs = "example.graph";
    private static boolean isGraphical = true;
    private static boolean writeFile = false;
    private static BufferedWriter bw = null;
    private static SimpleGraphViewWithJung sgv;

    public static void main(String[] args) throws UnsupportedEncodingException, IOException, RepositoryException,
            InterruptedException {
        ArrayList<String> graphNames = new ArrayList<String>();
        ArrayList<DirectedSparseGraph<MyNode, Integer>> graphs = new ArrayList<DirectedSparseGraph<MyNode, Integer>>();

        BufferedReader br = new BufferedReader(new FileReader(fileToGraphs));
        while (br.ready()) {
            graphNames.add(br.readLine());
            String edges = br.readLine();
            if (!edges.matches("")) {
                DirectedSparseGraph<MyNode, Integer> targetGraph = new DirectedSparseGraph<MyNode, Integer>();
                for (String node : edges.split("\t")) {
                    node = java.net.URLDecoder.decode(node, "UTF-8");
                    MyNode currentNode = new MyNode(node, 0, 0);
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
        for (int maxDepth = 2; maxDepth <= 2; maxDepth++) {
            for (double lambda = 0.2; lambda <= 1; lambda += 0.2) {
                for (int i = 0; i < graphs.size(); i++) {
                    // let SpreadActivation run
                    sa.run(spreadActivationThreshold, maxDepth, lambda, graphs.get(i));
                    Connectiveness c = new Connectiveness();
                    double fractionOfConnectedNodes = c.meassureConnectiveness(graphs.get(i));
                    System.out.println(graphNames.get(i) + "\t" + maxDepth + "\t" + lambda + "\t"
                            + fractionOfConnectedNodes);

                    if (writeFile) {
                        bw.write(graphNames.get(i) + "\t" + maxDepth + "\t" + lambda + "\t" + fractionOfConnectedNodes);
                        bw.newLine();
                        bw.flush();
                    }

                    if (isGraphical) {
                        sgv = new SimpleGraphViewWithJung();
                        sgv.showGraph(graphs.get(i));
                        Thread.sleep(100000);
                        sgv.close();
                    }
                }
            }
        }
        if (writeFile) {
            bw.close();
        }
    }

}
