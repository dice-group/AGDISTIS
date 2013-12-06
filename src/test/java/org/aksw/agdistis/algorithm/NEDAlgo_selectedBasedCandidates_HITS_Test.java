package org.aksw.agdistis.algorithm;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;

import org.aksw.agdistis.algorithm.lda.LDABasedNodeConfiguratorFactory;
import org.aksw.agdistis.webapp.GetDisambiguation;
import org.junit.Test;

import datatypeshelper.utils.doc.Document;
import datatypeshelper.utils.doc.ner.NamedEntitiesInText;
import datatypeshelper.utils.doc.ner.NamedEntityInText;

public class NEDAlgo_selectedBasedCandidates_HITS_Test {
    String languageTag = "en"; // de
    File dataDirectory = new File("/data/m.roeder/daten/dbpedia/3.9/AGDISTIS_Index"); // "/home/rusbeck/AGDISTIS/";
    String nodeType = "http://dbpedia.org/resource/";// "http://yago-knowledge.org/resource/"
    String edgeType = "http://dbpedia.org/ontology/";// "http://yago-knowledge.org/resource/"

    @Test
    public void testMinimalExample() throws InterruptedException, IOException, URISyntaxException {
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

        String preAnnotatedText = "<entity>" + obama + "</entity> visits <entity>" + merkel + "</entity> in <entity>"
                + city + "</entity>.";

        URL inferencerFile = LDABasedNodeConfiguratorFactory.class.getClassLoader().getResource("wiki_en.inferencer");
        URL pipeFile = LDABasedNodeConfiguratorFactory.class.getClassLoader().getResource("wiki_en.pipe");
        DisambiguationAlgorithm agdistis = NEDAlgo_selectedBasedCandidates_HITS.createAlgorithm(dataDirectory,
                new File(inferencerFile.toURI()), new File(pipeFile.toURI()), nodeType, edgeType);
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
            System.out.println((correct.get(namedEntity.getLabel()).equals(disambiguatedURL) ? "+ " : "- ")
                    + namedEntity.getLabel() + " (" + correct.get(namedEntity.getLabel()) + ") -> "
                    + disambiguatedURL);
            // Assert.assertEquals(correct.get(namedEntity.getLabel()), disambiguatedURL);
        }

    }

    @Test
    public void testFootballExample() throws InterruptedException, IOException, URISyntaxException {
        HashMap<String, String> correct = new HashMap<String, String>();
        correct.put("BARCELONA", "http://dbpedia.org/resource/FC_Barcelona");
        correct.put("ATLETICO", "http://dbpedia.org/resource/Atlético_Madrid");
        correct.put("SUPERCUP", "http://dbpedia.org/resource/Supercopa_de_España");
        correct.put("Barcelona", "http://dbpedia.org/resource/FC_Barcelona");
        correct.put("Atletico Madrid", "http://dbpedia.org/resource/Atlético_Madrid");
        correct.put("Spanish Supercup", "http://dbpedia.org/resource/Supercopa_de_España");
        correct.put("Ronaldo", "http://dbpedia.org/resource/Ronaldo");
        correct.put("Giovanni", "http://dbpedia.org/resource/Giovanni_Silva_de_Oliveira");
        correct.put("Pizzi", "http://dbpedia.org/resource/Juan_Antonio_Pizzi");
        correct.put("De la Pena", "http://dbpedia.org/resource/Iván_de_la_Peña");
        correct.put("Esnaider", "http://dbpedia.org/resource/Juan_Esnáider");
        correct.put("Pantic", "http://dbpedia.org/resource/Milinko_Pantić");

        String preAnnotatedText = " SOCCER - <entity>BARCELONA</entity> BEAT <entity>ATLETICO</entity>" +
                " 5-2 IN <entity>SUPERCUP</entity> .  <entity>BARCELONA</entity>" +
                " 1996-08-26  <entity>Barcelona</entity> beat <entity>Atletico Madrid</entity> 5-2 " +
                "( halftime 2-1 ) in the <entity>Spanish Supercup</entity> on Sunday :  Scorers :  " +
                "<entity>Barcelona</entity> - <entity>Ronaldo</entity> ( 5th and 89th minutes ) , " +
                "<entity>Giovanni</entity> ( 31st ) , <entity>Pizzi</entity> ( 73rd ) , " +
                "<entity>De la Pena</entity> ( 75th )  <entity>Atletico Madrid</entity> - " +
                "<entity>Esnaider</entity> ( 37th ) , <entity>Pantic</entity> ( 57th , penalty )" +
                "  Attendance 30,000  ";

        URL inferencerFile = LDABasedNodeConfiguratorFactory.class.getClassLoader().getResource("wiki_en.inferencer");
        URL pipeFile = LDABasedNodeConfiguratorFactory.class.getClassLoader().getResource("wiki_en.pipe");
        DisambiguationAlgorithm agdistis = NEDAlgo_selectedBasedCandidates_HITS.createAlgorithm(dataDirectory,
                new File(inferencerFile.toURI()), new File(pipeFile.toURI()), nodeType, edgeType);
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
            System.out.println((correct.get(namedEntity.getLabel()).equals(disambiguatedURL) ? "+ " : "- ")
                    + namedEntity.getLabel() + " (" + correct.get(namedEntity.getLabel()) + ") -> "
                    + disambiguatedURL);
            // Assert.assertEquals(correct.get(namedEntity.getLabel()), disambiguatedURL);
        }

    }
}
