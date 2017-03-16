import java.io.IOException;

import org.aksw.agdistis.graph.HITS;
import org.aksw.agdistis.graph.Node;
import org.junit.Test;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;

public class HitsTest {
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testSimpleHITS() throws InterruptedException, IOException {
		Graph g = new DirectedSparseMultigraph<Node, String>();
		Node a = new Node("dbpedia:Barack_Obama,_Sr.", 0, 0, "hits");
		Node b = new Node("dbpedia:Barack_Obama", 0, 0,"hits");
		Node c = new Node("dbpedia:Washington,_D.C.", 0, 0,"hits");
		Node d = new Node("dbpedia:Washington,_D.C._(novel)", 0, 0,"hits");

		Node e1 = new Node("dbpedia:Ann_Dunham", 0, 1,"hits");
		Node e2 = new Node("dbpedia:University_of_Hawaii", 0, 1,"hits");
		Node e3 = new Node("dbpedia:Hawaii", 0, 1,"hits");
		Node e4 = new Node("dbpedia:White_House", 0, 1,"hits");
		Node e5 = new Node("dbpedia:Federal_district", 0, 1,"hits");
		Node e6 = new Node("dbpedia:Elizabeth_II", 0, 1,"hits");
		Node e7 = new Node("dbpedia:London", 0, 1,"hits");
		Node e8 = new Node("dbpedia:New_York", 0, 1,"hits");
		Node e9 = new Node("dbpedia:United_Kingdom", 0, 1,"hits");
		Node e10 = new Node("dbpedia:Gore_Vidal", 0, 1,"hits");

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
	}
}
