import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;

import org.aksw.agdistis.algorithm.NEDAlgo_HITS;
import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.webapp.GetDisambiguation;
import org.junit.Test;

public class AGDISTISTest {

	@Test
	public void testUmlaute() throws InterruptedException, IOException {
		String taisho = "Emperor Taishō";
		String taishoURL = "http://dbpedia.org/resource/Emperor_Taishō";
		String japan = "Japan";
		String japanURL = "http://dbpedia.org/resource/Japan";

		HashMap<String, String> correct = new HashMap<String, String>();
		correct.put(taisho, taishoURL);
		correct.put(japan, japanURL);

		String preAnnotatedText = "<entity>" + taisho + "</entity> was the 123rd Emperor of <entity>" + japan + "</entity>.";

		NEDAlgo_HITS agdistis = new NEDAlgo_HITS();
		Document d = GetDisambiguation.textToDocument(preAnnotatedText);
		agdistis.run(d);
		NamedEntitiesInText namedEntities = d.getNamedEntitiesInText();
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

		NEDAlgo_HITS agdistis = new NEDAlgo_HITS();
		Document d = GetDisambiguation.textToDocument(preAnnotatedText);
		agdistis.run(d);
		NamedEntitiesInText namedEntities = d.getNamedEntitiesInText();
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
