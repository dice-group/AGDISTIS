import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.aksw.agdistis.graph.Node;
import org.aksw.agdistis.graph.PageRank;
import org.junit.Test;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;

public class PageRankTest {

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void oneGetsMost() throws IOException {

		Graph g = new DirectedSparseMultigraph<Node, String>();

		Node m = new Node("Master", 0, 0);
		Node s1 = new Node("Slave1", 0, 0);
		Node s2 = new Node("Slave2", 0, 0);
		Node s3 = new Node("Slave3", 0, 0);
		Node s4 = new Node("Slave4", 0, 0);

		g.addEdge("e1", s1, m);
		g.addEdge("e2", s2, m);
		g.addEdge("e3", s3, m);
		g.addEdge("e4", s4, m);
		g.addEdge("self", m, m); // node to self: this edge prevents
									// randomWalker

		PageRank pr = new PageRank();
		pr.runPr(g, 100, 0.001);

		// System.out.println(m + " ( = 1/5 * 0.15 + 0.85)");
		// System.out.println(s1 + " ( = 1/5 * 0.15)");
		// System.out.println(s2 + " (usw.)");
		// System.out.println(s3);
		// System.out.println(s4);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void soEqual() throws IOException {

		Graph g = new DirectedSparseMultigraph<Node, String>();

		Node n1 = new Node("N1", 0, 0);
		Node n2 = new Node("N2", 0, 0);
		Node n3 = new Node("N3", 0, 0);
		Node n4 = new Node("N4", 0, 0);

		g.addEdge("e1", n1, n2);
		g.addEdge("e2", n2, n3);
		g.addEdge("e3", n3, n4);
		g.addEdge("e4", n4, n1);

		PageRank pr = new PageRank();
		pr.runPr(g, 100, 0.001);

		assertTrue(n1.getPageRank() == 0.25);
		assertTrue(n2.getPageRank() == n3.getPageRank());
		assertTrue(n3.getPageRank() == n4.getPageRank());
		assertTrue(n4.getPageRank() == n1.getPageRank());
		// System.out.println("EQUAL1: " + n1);

	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void soEqualByRandomWalker() throws IOException {

		Graph g = new DirectedSparseMultigraph<Node, String>();

		Node n1 = new Node("N1", 0, 0);
		Node n2 = new Node("N2", 0, 0);
		Node n3 = new Node("N3", 0, 0);
		Node n4 = new Node("N4", 0, 0);

		g.addVertex(n1);
		g.addVertex(n2);
		g.addVertex(n3);
		g.addVertex(n4);

		PageRank pr = new PageRank();
		pr.runPr(g, 100, 0.001);

		assertTrue(n1.getPageRank() == 0.25);
		assertTrue(n2.getPageRank() == n3.getPageRank());
		assertTrue(n3.getPageRank() == n4.getPageRank());
		assertTrue(n4.getPageRank() == n1.getPageRank());

		// System.out.println("EQUAL2: " + n1);
	}

}
