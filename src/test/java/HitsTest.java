import org.junit.Test;

import de.bluekiwi.labs.graph.HITS;
import de.bluekiwi.labs.vis.MyNode;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;

public class HitsTest {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testSimpleHITS() throws InterruptedException {
        Graph g = new DirectedSparseMultigraph<MyNode, String>();
        MyNode a = new MyNode("dbpedia:Barack_Obama,_Sr.", 0, 0);
        MyNode b = new MyNode("dbpedia:Barack_Obama", 0, 0);
        MyNode c = new MyNode("dbpedia:Washington,_D.C.", 0, 0);
        MyNode d = new MyNode("dbpedia:Washington,_D.C._(novel)", 0, 0);

        MyNode e1 = new MyNode("dbpedia:Ann_Dunham", 0, 1);
        MyNode e2 = new MyNode("dbpedia:University_of_Hawaii", 0, 1);
        MyNode e3 = new MyNode("dbpedia:Hawaii", 0, 1);
        MyNode e4 = new MyNode("dbpedia:White_House", 0, 1);
        MyNode e5 = new MyNode("dbpedia:Federal_district", 0, 1);
        MyNode e6 = new MyNode("dbpedia:Elizabeth_II", 0, 1);
        MyNode e7 = new MyNode("dbpedia:London", 0, 1);
        MyNode e8 = new MyNode("dbpedia:New_York", 0, 1);
        MyNode e9 = new MyNode("dbpedia:United_Kingdom", 0, 1);
        MyNode e10 = new MyNode("dbpedia:Gore_Vidal", 0, 1);

        g.addEdge("dbpedia:Barack_Obama,_Sr.", a, b);
        g.addEdge("dbpedia:Barack_Obama,_Sr.1", a, e1);
        g.addEdge("dbpedia:Barack_Obama,_Sr.2", a, e2);
        g.addEdge("dbpedia:Barack_Obama,_Sr.3", e1, a);
        g.addEdge("dbpedia:Barack_Obama,_Sr.4", e2, a);

        g.addEdge("dbpedia:Barack_Obama", b, e3);
        g.addEdge("dbpedia:Barack_Obama1", b, e4);
        g.addEdge("dbpedia:Barack_Obama2", e3, b);

        g.addEdge("dbpedia:Washington,_D.C.", c, e5);
        g.addEdge("dbpedia:Washington,_D.C.1", e5, c);

        g.addEdge("dbpedia:Washington,_D.C._(novel)", d, e10);
        g.addEdge("dbpedia:Washington,_D.C._(novel)1", e10, d);
        g.addEdge("dbpedia:Washington,_D.C._(novel)2", d, e9);

        g.addEdge("dbpedia:United_Kingdom", e9, e6);
        g.addEdge("dbpedia:United_Kingdom2", e9, e7);
        g.addEdge("dbpedia:United_Kingdom3", e7, e9);

        g.addEdge("dbpedia:Gore_Vidal", e10, e8);

        g.addEdge("dbpedia:White_House", e4, c);
        g.addEdge("dbpedia:White_House1", e4, b);

        g.addEdge("dbpedia:Ann_Dunham", e1, e2);
        g.addEdge("dbpedia:University_of_Hawaii", e2, e3);

        HITS algo = new HITS();
        int k = 20;
        algo.runHits(g, k);
        for (Object m : g.getVertices()) {
            MyNode mm = (MyNode) m;
            System.out.println(mm.getCandidateURI() + " x: " + mm.getAuthorityWeight() + " y: " + mm.getHubWeight());
        }

    }
}
