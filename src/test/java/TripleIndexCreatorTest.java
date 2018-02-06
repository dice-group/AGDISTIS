
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.aksw.agdistis.algorithm.NEDAlgo_HITS;
import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.util.TripleIndex;
import org.aksw.agdistis.util.TripleIndexCreator;
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
      index = new TripleIndex(AGDISTISTest.CFG);

    } catch (final IOException e) {
      log.error(
          "Can not load index or DBpedia repository due to either wrong properties in agdistis.properties or missing index at location",
          e);
    }
  }

  @After
  public void close() {
    try {
      index.close();
    } catch (final IOException e) {
      log.error(
          "Can not load index or DBpedia repository due to either wrong properties in agdistis.properties or missing index at location",
          e);
    }
  }

  @Test
  /**
   * tests https://github.com/dice-group/AGDISTIS/issues/46 if we need to return URIs from two
   * different KBs AGDISTIS returns breaks
   *
   * @throws IOException
   */
  public void testMinimalOntologyExample() throws IOException {
    // load test data into index
    final TripleIndexCreator tic = new TripleIndexCreator();
    final File file = new File("src/test/resources/test_evertec.ttl");
    final File folder = new File("src/test/resources/evertec");
    if (folder.exists()) {
      folder.delete();
    }
    tic.createIndex(Lists.newArrayList(file), folder.getAbsolutePath(), null);

    // set the properties correctly

    final NEDAlgo_HITS agdistis = new NEDAlgo_HITS(AGDISTISTest.CFG);
    agdistis.setNodeType("http://fairhair.ai/kg/resource/");
    agdistis.setEdgeType("http://dbpedia.org/ontology/");

    // load index
    index.setIndex(folder.getAbsolutePath());
    agdistis.setIndex(index);

    // test index
    final String taisho = "Evertec";
    final String taishoURL = "http://fairhair.ai/kg/resource/Evertec";
    final String japan = "Puerto Rico";
    final String japanURL = "http://dbpedia.org/resource/Puerto_Rico";

    final HashMap<String, String> correct = new HashMap<String, String>();
    correct.put(taisho, taishoURL);
    correct.put(japan, japanURL);
    final String preAnnotatedText =
        "<entity>" + taisho + "</entity> is a company in<entity>" + japan + "</entity>.";

    Document d = GetDisambiguation.textToDocument(preAnnotatedText);
    agdistis.run(d, null);

    NamedEntitiesInText namedEntities = d.getNamedEntitiesInText();
    HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
    for (final NamedEntityInText namedEntity : namedEntities) {
      final String disambiguatedURL = namedEntity.getNamedEntityUri();
      System.out.println(namedEntity);
      results.put(namedEntity, disambiguatedURL);
    }
    for (final NamedEntityInText namedEntity : results.keySet()) {
      namedEntity.getNamedEntityUri();
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
    for (final NamedEntityInText namedEntity : namedEntities) {
      final String disambiguatedURL = namedEntity.getNamedEntityUri();
      System.out.println(namedEntity);
      results.put(namedEntity, disambiguatedURL);
    }
    for (final NamedEntityInText namedEntity : results.keySet()) {
      namedEntity.getNamedEntityUri();
      System.out.println(namedEntity.getLabel() + " -> " + results.get(namedEntity));
      // TODO comment that line in
      // assertTrue(correct.get(namedEntity.getLabel()).equals(disambiguatedURL));
    }

  }

}
