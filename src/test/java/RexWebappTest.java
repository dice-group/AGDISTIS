import java.io.IOException;
import java.util.HashMap;

import org.aksw.agdistis.webapp.GetDisambiguation;
import org.junit.Test;

import datatypeshelper.utils.doc.ner.NamedEntityInText;

public class RexWebappTest {
	@Test
	public void testWebAppForRex() throws InterruptedException, IOException {
		String subject = "Onome Sodje";
		String object = "Barnsley";
		String predicate = "http://dbpedia.org/ontology/team";
		String preAnnotatedText = "<entity>" + subject + "</entity><entity>" + object + "</entity>.";

		GetDisambiguation algo = new GetDisambiguation("/Users/ricardousbeck/index_dbpedia_en_39");

		HashMap<NamedEntityInText, String> results = algo.processDocument(subject, predicate, object, preAnnotatedText);
		for (NamedEntityInText namedEntity : results.keySet()) {
			String disambiguatedURL = results.get(namedEntity);
			System.out.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
		}
		algo.close();
	}
}
