import java.io.IOException;
import java.util.HashMap;

import org.aksw.agdistis.webapp.AGDISTIS;
import org.junit.Test;

import datatypeshelper.utils.doc.ner.NamedEntityInText;

public class AGDISTISTest {
	@Test
	public void testUmlaute() throws InterruptedException, IOException {
		// String osumi = "Masaaki Ōsumi";
		String osumi = "Masaaki Ôsumi";
		String movie = "La Seine no Hoshi";

		String preAnnotatedText = "<entity>" + osumi + "</entity> directed <entity>" + movie + "</entity>.";

		String modelDirectory = "/data/r.usbeck";

		AGDISTIS agdistis = new AGDISTIS(modelDirectory);
		HashMap<NamedEntityInText, String> results = agdistis.runDisambiguation(preAnnotatedText);
		for (NamedEntityInText namedEntity : results.keySet()) {
			String disambiguatedURL = results.get(namedEntity);
			System.out.println(disambiguatedURL);
		}

	}
}
