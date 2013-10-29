package org.aksw.agdistis.webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.aksw.agdistis.algorithm.DisambiguationAlgorithm;
import org.aksw.agdistis.algorithm.NEDAlgo_HITS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datatypeshelper.utils.doc.Document;
import datatypeshelper.utils.doc.DocumentText;
import datatypeshelper.utils.doc.ner.NamedEntitiesInText;
import datatypeshelper.utils.doc.ner.NamedEntityInText;

public class AGDISTIS {
    private static Logger log = LoggerFactory.getLogger(AGDISTIS.class);
    private DisambiguationAlgorithm algo = null;

    public AGDISTIS(String modelDirectory) {
        String knowledgeBase = "http://dbpedia.org/resource/";
        double threshholdTrigram = 1;
        int maxDepth = 2;
        algo = new NEDAlgo_HITS(modelDirectory, knowledgeBase);
        algo.setMaxDepth(maxDepth);
        algo.setThreshholdTrigram(threshholdTrigram);
    }

    public static void main(String[] args) throws IOException {
        String preAnnotatedText = "<entity>Barack Obama</entity>  meets <entity>Angela Merkel</entity>  in <entity>Berlin</entity>  to discuss a <entity>new world order</entity> ";

         String modelDirectory = "/Users/ricardousbeck/AGDISTIS";
//     String modelDirectory = "/data/r.usbeck";

        AGDISTIS agdistis = new AGDISTIS(modelDirectory);
        HashMap<NamedEntityInText, String> results = agdistis.runDisambiguation(preAnnotatedText);
        for (NamedEntityInText namedEntity : results.keySet()) {
            String disambiguatedURL = results.get(namedEntity);
            log.info(namedEntity + " ==> " + disambiguatedURL);
        }
    }

    public HashMap<NamedEntityInText, String> runDisambiguation(String preAnnotatedText) throws IOException {
      

        Document document = new Document();
        ArrayList<NamedEntityInText> list = new ArrayList<NamedEntityInText>();
        log.info("\tText: " + preAnnotatedText);
        for (int c = 0; c < preAnnotatedText.length(); c++) {
            if (preAnnotatedText.length() > c + 8) {
                if (preAnnotatedText.substring(c, c + 8).equals("<entity>")) {
                    c += 8;
                    int beginIndex = c;
                    int endIndex = preAnnotatedText.indexOf("</entity>", c);
                    String label = preAnnotatedText.substring(beginIndex, endIndex);
                    log.info("\t" + beginIndex + " " + endIndex + " " + label);
                    list.add(new NamedEntityInText(beginIndex, endIndex - beginIndex, label));
                }
            }
        }
        NamedEntitiesInText nes = new  NamedEntitiesInText(list);
        DocumentText text = new DocumentText(preAnnotatedText);

        document.addProperty(text);
        document.addProperty(nes);
        algo.run(document);
        
        NamedEntitiesInText namedEntities = document.getProperty(NamedEntitiesInText.class);
        HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
        for (NamedEntityInText namedEntity : namedEntities) {
            String disambiguatedURL = algo.findResult(namedEntity);
            results.put(namedEntity, disambiguatedURL);
        }
        return results;

    }

    public void close() {
        algo.close();
    }
}