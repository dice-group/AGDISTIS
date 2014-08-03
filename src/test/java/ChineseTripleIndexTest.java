import org.aksw.agdistis.util.Triple;
import org.aksw.agdistis.util.TripleIndex;
import org.apache.lucene.search.spell.NGramDistance;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class ChineseTripleIndexTest {
	Logger log = LoggerFactory.getLogger(ChineseTripleIndexTest.class);
	private TripleIndex index;

	@Before
	public void init() {
		index = new TripleIndex(new File("E:\\project\\gsoc2014\\dbpedia3.9\\zh_index"));
	}

	@After
	public void close() {
		index.close();
	}

	@Test
	public void testRedirects() {
		String candidateURL = "http://dbpedia.org/resource/奥巴马";
		List<Triple> redirect = index.search(candidateURL, "http://dbpedia.org/ontology/wikiPageRedirects", null);
		for (Triple t : redirect) {
			log.debug(t.toString());
		}
		assertTrue(redirect.size() > 0);
		candidateURL = "http://dbpedia.org/resource/贝拉克·奥巴马";
		redirect = index.search(candidateURL, "http://dbpedia.org/ontology/wikiPageRedirects", null);
		assertTrue(redirect.size() == 0);
	}

	@Test
	public void testType() {
		String candidateURL = "http://dbpedia.org/resource/巴拉克·奥巴马";
		List<Triple> type = index.search(candidateURL, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", null);
		assertTrue(type.size() > 0);
		for (Triple t : type) {
			log.debug(t.toString());
		}
	}

	@Test
	public void testRdfsLabel() {
		String candidateURL = "http://dbpedia.org/resource/巴拉克·奥巴马";
		List<Triple> type = index.search(candidateURL, "http://www.w3.org/2000/01/rdf-schema#label", null);
		assertTrue(type.size() > 0);
		for (Triple t : type) {
			log.debug(t.toString());
		}
	}

	@Test
	public void testMultipleTermsPerField() {
		String candidate = "奥巴马";
		List<Triple> type = index.search(null, "http://www.w3.org/2000/01/rdf-schema#label", candidate);
		assertTrue(type.size() > 1);
		for (Triple t : type) {
			log.debug(t.toString());
            System.out.println(t.toString());
		}
	}
}
