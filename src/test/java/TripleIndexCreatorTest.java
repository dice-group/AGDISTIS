
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.aksw.agdistis.algorithm.NEDAlgo_HITS;
import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.index.indexImpl.TripleIndex;
import org.aksw.agdistis.indexWriter.TripleIndexCreator;
import org.aksw.agdistis.webapp.GetDisambiguation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class TripleIndexCreatorTest {

	Logger log = LoggerFactory.getLogger(TripleIndexCreatorTest.class);
	private TripleIndex index;

	@Before
	public void init() {
		try {
			index = new TripleIndex();

		} catch (IOException e) {
			log.error(
					"Can not load index or DBpedia repository due to either wrong properties in agdistis.properties or missing index at location",
					e);
		}
	}

	@After
	public void close() {
		try {
			index.close();
		} catch (IOException e) {
			log.error(
					"Can not load index or DBpedia repository due to either wrong properties in agdistis.properties or missing index at location",
					e);
		}
	}

	@Test
	/**
	 * tests https://github.com/dice-group/AGDISTIS/issues/46 if we need to return
	 * URIs from two different KBs AGDISTIS returns breaks
	 * 
	 * @throws IOException
	 */
	public void testMinimalOntologyExample() throws IOException {
		// load test data into index
		TripleIndexCreator tic = new TripleIndexCreator();
		File file = new File("src/test/resources/test_evertec.ttl");
		File folder = new File("src/test/resources/evertec");
		if (folder.exists()) {
			folder.delete();
		}
		tic.createIndex(Lists.newArrayList(file), folder.getAbsolutePath(), null,false);

		// set the properties correctly

		NEDAlgo_HITS agdistis = new NEDAlgo_HITS();
		agdistis.setNodeType("http://fairhair.ai/kg/resource/");
		agdistis.setEdgeType("http://dbpedia.org/ontology/");

		// load index
		index.setIndex(folder.getAbsolutePath());
		agdistis.setIndex(index);

		// test index
		String taisho = "Evertec";
		String taishoURL = "http://fairhair.ai/kg/resource/Evertec";
		String japan = "Puerto Rico";
		String japanURL = "http://dbpedia.org/resource/Puerto_Rico";

		HashMap<String, String> correct = new HashMap<String, String>();
		correct.put(taisho, taishoURL);
		correct.put(japan, japanURL);
		String preAnnotatedText = "<entity>" + taisho + "</entity> is a company in<entity>" + japan + "</entity>.";

		Document d = GetDisambiguation.textToDocument(preAnnotatedText);
		agdistis.run(d, null);

		NamedEntitiesInText namedEntities = d.getNamedEntitiesInText();
		HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
		for (NamedEntityInText namedEntity : namedEntities) {
			String disambiguatedURL = namedEntity.getNamedEntityUri();
			System.out.println(namedEntity);
			results.put(namedEntity, disambiguatedURL);
		}
		for (NamedEntityInText namedEntity : results.keySet()) {
			String disambiguatedURL = namedEntity.getNamedEntityUri();
			System.out.println(namedEntity.getLabel() + " -> " + results.get(namedEntity));
			// TODO comment that line in
			// assertTrue(correct.get(namedEntity.getLabel()).equals(disambiguatedURL));
		}

		// TODO if we change the node type, Puerto Rico gets found but not Evertec
		agdistis.setNodeType("http://dbpedia.org/resource/");

		d = GetDisambiguation.textToDocument(preAnnotatedText);
		agdistis.run(d, null);

		namedEntities = d.getNamedEntitiesInText();
		results = new HashMap<NamedEntityInText, String>();
		for (NamedEntityInText namedEntity : namedEntities) {
			String disambiguatedURL = namedEntity.getNamedEntityUri();
			System.out.println(namedEntity);
			results.put(namedEntity, disambiguatedURL);
		}
		for (NamedEntityInText namedEntity : results.keySet()) {
			String disambiguatedURL = namedEntity.getNamedEntityUri();
			System.out.println(namedEntity.getLabel() + " -> " + results.get(namedEntity));
			// TODO comment that line in
			// assertTrue(correct.get(namedEntity.getLabel()).equals(disambiguatedURL));
		}

	}

}
