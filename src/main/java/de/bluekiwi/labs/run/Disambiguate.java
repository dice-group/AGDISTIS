package de.bluekiwi.labs.run;

import java.io.IOException;

import com.unister.semweb.topicmodeling.utils.doc.Document;
import com.unister.semweb.topicmodeling.utils.doc.DocumentText;
import com.unister.semweb.topicmodeling.utils.doc.ner.NamedEntitiesInText;
import com.unister.semweb.topicmodeling.utils.doc.ner.NamedEntityInText;

import de.bluekiwi.labs.algorithm.NEDAlgo_HITS;

public class Disambiguate {

    public static void main(String[] args) throws IOException {

        entest();
        // deTest();
    }

    private static void deTest() {

        String languageTag = "de"; // de,en
        String dataDirectory = "/Users/gerb/Development/workspaces/experimental/agdistis/en";
        int maxDepth = 2;
        double threshholdTrigram = 0.835;

        // DISAMBIGUATION ALGORITHM
        NEDAlgo_HITS algo = new NEDAlgo_HITS(1, languageTag, dataDirectory, "http://dbpedia.org/resource/");
        // NEDAlgo_wihtoutGraphTechniques algo = new
        // NEDAlgo_wihtoutGraphTechniques(corpus.getNumberOfDocuments());

        Document document = new Document();
        NamedEntitiesInText nes = new NamedEntitiesInText(new NamedEntityInText(37, 15, "Golf_Geo/Mexiko"), new NamedEntityInText(28, 7, "Golf_Car/VI"), new NamedEntityInText(9, 9, "Jaguar_Car/RX"));
        DocumentText text = new DocumentText("Der neue Jaguar RX ist kein Golf VI. Golf von Mexiko");

        document.addProperty(text);
        document.addProperty(nes);

        algo.run(document, threshholdTrigram, maxDepth);
        // algo.run(document, threshholdTrigram); --> used for algo without
        // graph techniques
        NamedEntitiesInText namedEntities = document.getProperty(NamedEntitiesInText.class);
        for (NamedEntityInText namedEntity : namedEntities) {
            String disambiguatedURL = algo.findResult(namedEntity);
            String label = namedEntity.getLabel();
            System.out.println(label + " ->  " + disambiguatedURL);
        }

        algo.close();
    }

    private static void entest() {

        String languageTag = "en"; // de,en
        String dataDirectory = "/Users/gerb/Development/workspaces/experimental/agdistis/en";
        int maxDepth = 2;
        double threshholdTrigram = 0.835;

        NamedEntitiesInText nes = new NamedEntitiesInText(
                new NamedEntityInText(38, 8, "Lovelace"),
                new NamedEntityInText(62, 11, "Rob Epstein"),
                new NamedEntityInText(78, 16, "Jeffery Friedman"),
                new NamedEntityInText(101, 9, "Admission"),
                new NamedEntityInText(126, 10, "Paul Weitz"));

        String sentence = "Recent work includes the 2013 films ``Lovelace,'' directed by Rob Epstein and Jeffery Friedman and ``Admission,'' directed by Paul Weitz.";

        Document document = new Document();
        DocumentText text = new DocumentText(sentence);

        document.addProperty(text);
        document.addProperty(nes);

        NEDAlgo_HITS algo = new NEDAlgo_HITS(1, languageTag, dataDirectory, "http://dbpedia.org/resource/");

        algo.run(document, threshholdTrigram, maxDepth);
        // algo.run(document, threshholdTrigram); --> used for algo without
        // graph techniques
        NamedEntitiesInText namedEntities = document.getProperty(NamedEntitiesInText.class);
        for (NamedEntityInText namedEntity : namedEntities) {
            String disambiguatedURL = algo.findResult(namedEntity);
            String label = namedEntity.getLabel();
            System.out.println(label + " ->  " + disambiguatedURL);
        }

        algo.close();
    }

}
