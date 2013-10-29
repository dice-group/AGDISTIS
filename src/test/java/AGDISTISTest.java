import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;

import org.aksw.agdistis.algorithm.DisambiguationAlgorithm;
import org.junit.Test;

import datatypeshelper.utils.doc.ner.NamedEntityInText;

public class AGDISTISTest {
	@Test
	public void testUmlaute() throws InterruptedException, IOException {
		String osumi = "Masaaki Ōsumi";
		String osumiURL = "http://dbpedia.org/resource/Masaaki_Ōsumi";
		String movie = "La Seine no Hoshi";
		String movieURL = "http://dbpedia.org/resource/La_Seine_no_Hoshi";

		HashMap<String, String> correct = new HashMap<String, String>();
		correct.put(osumi, osumiURL);
		correct.put(movie, movieURL);

		String preAnnotatedText = "<entity>" + osumi + "</entity> directed <entity>" + movie + "</entity>.";

		String modelDirectory = "/data/r.usbeck";

		DisambiguationAlgorithm agdistis = new AGDISTIS(modelDirectory);
		HashMap<NamedEntityInText, String> results = agdistis.runDisambiguation(preAnnotatedText);
		for (NamedEntityInText namedEntity : results.keySet()) {
			String disambiguatedURL = results.get(namedEntity);
			System.out.println(namedEntity.getLabel() +" -> " + disambiguatedURL );
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

		String modelDirectory = "/data/r.usbeck";
		AGDISTIS agdistis = new AGDISTIS(modelDirectory);
		HashMap<NamedEntityInText, String> results = agdistis.runDisambiguation(preAnnotatedText);
		for (NamedEntityInText namedEntity : results.keySet()) {
			String disambiguatedURL = results.get(namedEntity);
			System.out.println(namedEntity.getLabel() +" -> " + disambiguatedURL );
			assertTrue(correct.get(namedEntity.getLabel()).equals(disambiguatedURL));
		}

	}
	
}
