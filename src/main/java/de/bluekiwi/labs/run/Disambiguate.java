package de.bluekiwi.labs.run;

import java.io.IOException;

import com.unister.semweb.topicmodeling.utils.doc.Document;
import com.unister.semweb.topicmodeling.utils.doc.DocumentText;
import com.unister.semweb.topicmodeling.utils.doc.ner.NamedEntitiesInText;
import com.unister.semweb.topicmodeling.utils.doc.ner.NamedEntityInText;

import de.bluekiwi.labs.algorithm.NEDAlgo_HITS;

public class Disambiguate {

    public static void main(String[] args) throws IOException {
        String languageTag = "de"; // de,en
        String dataDirectory = "/data/r.usbeck";
        int maxDepth = 2;
        double threshholdTrigram = 0.835;

        Document document = new Document();
        NamedEntitiesInText nes = new NamedEntitiesInText(new NamedEntityInText(37, 15, "Golf_Geo/Mexiko"),
                new NamedEntityInText(28, 7, "Golf_Car/VI"),
                new NamedEntityInText(9, 9, "Jaguar_Car/RX"));
        DocumentText text = new DocumentText("Der neue Jaguar RX ist kein Golf VI. Golf von Mexiko");

        document.addProperty(text);
        document.addProperty(nes);

        // DISAMBIGUATION ALGORITHM
        NEDAlgo_HITS algo = new NEDAlgo_HITS(1, languageTag, dataDirectory);
        // NEDAlgo_wihtoutGraphTechniques algo = new NEDAlgo_wihtoutGraphTechniques(corpus.getNumberOfDocuments());
        algo.run(document, threshholdTrigram, maxDepth);
        // algo.run(document, threshholdTrigram); --> used for algo without graph techniques

        NamedEntitiesInText namedEntities = document.getProperty(NamedEntitiesInText.class);
        for (NamedEntityInText namedEntity : namedEntities) {
            String disambiguatedURL = algo.findResult(namedEntity);
            String label = namedEntity.getLabel();
            System.out.println(label + " ->  " + disambiguatedURL);
        }

        algo.close();
    }
}