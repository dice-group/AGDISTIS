
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

import org.aksw.agdistis.algorithm.CandidateUtil;
import org.aksw.agdistis.algorithm.NEDAlgo_HITS;
import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.webapp.GetDisambiguation;
import org.junit.Test;

public class AGDISTISTest {
  static String CFG = "config/agdistis.properties";

  @Test
  public void testUmlaute() throws InterruptedException, IOException {
    final String taisho = "Emperor Taishō";
    final String taishoURL = "http://dbpedia.org/resource/Emperor_Taishō";
    final String japan = "Japan";
    final String japanURL = "http://dbpedia.org/resource/Japan";

    final HashMap<String, String> correct = new HashMap<String, String>();
    correct.put(taisho, taishoURL);
    correct.put(japan, japanURL);
    final String preAnnotatedText =
        "<entity>" + taisho + "</entity> was the 123rd Emperor of <entity>" + japan + "</entity>.";

    final NEDAlgo_HITS agdistis = new NEDAlgo_HITS(AGDISTISTest.CFG);
    final Document d = GetDisambiguation.textToDocument(preAnnotatedText);
    agdistis.run(d, null);

    final NamedEntitiesInText namedEntities = d.getNamedEntitiesInText();
    final HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
    for (final NamedEntityInText namedEntity : namedEntities) {
      final String disambiguatedURL = namedEntity.getNamedEntityUri();
      results.put(namedEntity, disambiguatedURL);
    }
    for (final NamedEntityInText namedEntity : namedEntities) {
      final String disambiguatedURL = namedEntity.getNamedEntityUri();
      System.out.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
      assertTrue(correct.get(namedEntity.getLabel()).equals(disambiguatedURL));
    }

  }

  @Test
  public void testMinimalExample() throws InterruptedException, IOException {
    final String obama = "Barack Obama";
    final String obamaURL = "http://dbpedia.org/resource/Barack_Obama";
    final String merkel = "Angela Merkel";
    final String merkelURL = "http://dbpedia.org/resource/Angela_Merkel";
    final String city = "Berlin";
    final String cityURL = "http://dbpedia.org/resource/Berlin";

    final HashMap<String, String> correct = new HashMap<String, String>();
    correct.put(obama, obamaURL);
    correct.put(merkel, merkelURL);
    correct.put(city, cityURL);

    final String preAnnotatedText = "<entity>" + obama + "</entity> visits <entity>" + merkel
        + "</entity> in <entity>" + city + "</entity>.";

    final NEDAlgo_HITS agdistis = new NEDAlgo_HITS(AGDISTISTest.CFG);
    final Document d = GetDisambiguation.textToDocument(preAnnotatedText);
    agdistis.run(d, null);

    final NamedEntitiesInText namedEntities = d.getNamedEntitiesInText();
    final HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
    for (final NamedEntityInText namedEntity : namedEntities) {
      final String disambiguatedURL = namedEntity.getNamedEntityUri();
      results.put(namedEntity, disambiguatedURL);
    }
    for (final NamedEntityInText namedEntity : namedEntities) {
      final String disambiguatedURL = namedEntity.getNamedEntityUri();
      System.out.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
      assertTrue(correct.get(namedEntity.getLabel()).equals(disambiguatedURL));
    }

  }

  @Test
  /**
   * This test ensures that entities consiting only of punctations, which are later reduced to a
   * string of length 0 by
   * https://github.com/dice-group/AGDISTIS/blob/master/src/main/java/org/aksw/agdistis/util/
   * Stemming.java#L91 are not causing exceptions
   *
   * @throws InterruptedException
   * @throws IOException
   */
  public void testinterpunctation() throws InterruptedException, IOException {
    final String question = "???";
    final String questionURL = "???";

    final HashMap<String, String> correct = new HashMap<String, String>();
    correct.put(question, questionURL);

    final String preAnnotatedText = "<entity>???</entity>.";

    final NEDAlgo_HITS agdistis = new NEDAlgo_HITS(AGDISTISTest.CFG);
    final Document d = GetDisambiguation.textToDocument(preAnnotatedText);
    agdistis.run(d, null);

    final NamedEntitiesInText namedEntities = d.getNamedEntitiesInText();
    final HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
    for (final NamedEntityInText namedEntity : namedEntities) {
      final String disambiguatedURL = namedEntity.getNamedEntityUri();
      results.put(namedEntity, disambiguatedURL);
    }
    for (final NamedEntityInText namedEntity : namedEntities) {
      final String disambiguatedURL = namedEntity.getNamedEntityUri();
      System.out.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
      assertTrue(correct.get(namedEntity.getLabel()).equals(disambiguatedURL));
    }

  }

  @Test
  public void testContext() throws InterruptedException, IOException {
    final String angelina = "Angelina";
    final String angelinaURL = "http://dbpedia.org/resource/Angelina_Jolie";
    final String brad = "Brad";
    final String bradURL = "http://dbpedia.org/resource/Brad_Pitt";
    final String jon = "Jon";
    final String jonURL = "http://dbpedia.org/resource/Jon_Voight";

    // load properties to see if the context index exists
    final Properties prop = new Properties();
    final InputStream input =
        CandidateUtil.class.getResourceAsStream("/config/agdistis.properties");
    prop.load(input);
    final String envContext = System.getenv("AGDISTIS_CONTEXT");
    final Boolean context =
        Boolean.valueOf(envContext != null ? envContext : prop.getProperty("context"));
    if (context == true) { // in case the index by context exist

      final HashMap<String, String> correct = new HashMap<String, String>();
      correct.put(angelina, angelinaURL);
      correct.put(jon, jonURL);
      correct.put(brad, bradURL);

      final String preAnnotatedText = "<entity>" + angelina + "</entity>, her father <entity>" + jon
          + "</entity>, and her partner <entity>" + brad
          + "</entity> never played together in the same movie.";

      final NEDAlgo_HITS agdistis = new NEDAlgo_HITS(AGDISTISTest.CFG);
      final Document d = GetDisambiguation.textToDocument(preAnnotatedText);
      agdistis.run(d, null);

      final NamedEntitiesInText namedEntities = d.getNamedEntitiesInText();
      final HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
      for (final NamedEntityInText namedEntity : namedEntities) {
        final String disambiguatedURL = namedEntity.getNamedEntityUri();
        results.put(namedEntity, disambiguatedURL);
      }
      for (final NamedEntityInText namedEntity : namedEntities) {
        final String disambiguatedURL = namedEntity.getNamedEntityUri();
        System.out.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
        assertTrue(correct.get(namedEntity.getLabel()).equals(disambiguatedURL));
      }
    }
  }

}
