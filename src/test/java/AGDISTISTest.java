import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.aksw.agdistis.algorithm.DisambiguationAlgorithm;
import org.aksw.agdistis.algorithm.NEDAlgo_HITS;
import org.aksw.agdistis.webapp.GetDisambiguation;
import org.junit.Test;

import datatypeshelper.utils.doc.Document;
import datatypeshelper.utils.doc.ner.NamedEntitiesInText;
import datatypeshelper.utils.doc.ner.NamedEntityInText;

public class AGDISTISTest {
	String languageTag = "en"; // de
	File dataDirectory = new File("/data/r.usbeck/indexdbpedia_en/"); // "/home/rusbeck/AGDISTIS/";
	String nodeType = "http://dbpedia.org/resource/";// "http://yago-knowledge.org/resource/"
	String edgeType = "http://dbpedia.org/ontology/";// "http://yago-knowledge.org/resource/"

	@Test
	public void testUmlaute() throws InterruptedException, IOException {
		String osumi = "Masaaki Ōsumi";
		String osumiURL = "http://dbpedia.org/resource/Masaaki_Ōsumi";
		String japan = "Japan";
		String japanURL = "http://dbpedia.org/resource/Japan";

		HashMap<String, String> correct = new HashMap<String, String>();
		correct.put(osumi, osumiURL);
		correct.put(japan, japanURL);

		String preAnnotatedText = "<entity>" + osumi + "</entity> works in <entity>" + japan + "</entity>.";

		DisambiguationAlgorithm agdistis = new NEDAlgo_HITS(dataDirectory, nodeType, edgeType);
		Document d = GetDisambiguation.textToDocument(preAnnotatedText);
		agdistis.run(d);
		NamedEntitiesInText namedEntities = d.getProperty(NamedEntitiesInText.class);
		HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
		for (NamedEntityInText namedEntity : namedEntities) {
			String disambiguatedURL = agdistis.findResult(namedEntity);
			results.put(namedEntity, disambiguatedURL);
		}

		for (NamedEntityInText namedEntity : results.keySet()) {
			String disambiguatedURL = results.get(namedEntity);
			System.out.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
			assertTrue(correct.get(namedEntity.getLabel()).equals(disambiguatedURL));
		}

	}

	@Test
	public void testMinimalExample() throws InterruptedException, IOException {
		// String osumi = "Masaaki Ōsumi";
		String obama = "Barack Obama";
		String obamaURL = "http://dbpedia.org/resource/Barack_Obama";
		String merkel = "Angela Merkel";
		String merkelURL = "http://dbpedia.org/resource/Angela_Merkel";
		String city = "Berlin";
		String cityURL = "http://dbpedia.org/resource/Berlin";

		HashMap<String, String> correct = new HashMap<String, String>();
		correct.put(obama, obamaURL);
		correct.put(merkel, merkelURL);
		correct.put(city, cityURL);

		String preAnnotatedText = "<entity>" + obama + "</entity> visits <entity>" + merkel + "</entity> in <entity>" + city + "</entity>.";

		DisambiguationAlgorithm agdistis = new NEDAlgo_HITS(  dataDirectory, nodeType, edgeType);
		Document d = GetDisambiguation.textToDocument(preAnnotatedText);
		agdistis.run(d);
		NamedEntitiesInText namedEntities = d.getProperty(NamedEntitiesInText.class);
		HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
		for (NamedEntityInText namedEntity : namedEntities) {
			String disambiguatedURL = agdistis.findResult(namedEntity);
			results.put(namedEntity, disambiguatedURL);
		}
		for (NamedEntityInText namedEntity : results.keySet()) {
			String disambiguatedURL = results.get(namedEntity);
			System.out.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
			assertTrue(correct.get(namedEntity.getLabel()).equals(disambiguatedURL));
		}

	}
}
