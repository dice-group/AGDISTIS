package org.aksw.agdistis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unister.semweb.topicmodeling.utils.doc.Document;
import com.unister.semweb.topicmodeling.utils.doc.DocumentText;
import com.unister.semweb.topicmodeling.utils.doc.ner.NamedEntitiesInText;
import com.unister.semweb.topicmodeling.utils.doc.ner.NamedEntityInText;

import de.bluekiwi.labs.algorithm.NEDAlgo_HITS;

public class AGDISTIS {
    private static Logger log = LoggerFactory.getLogger(AGDISTIS.class);
    private NEDAlgo_HITS algo = null;

    public AGDISTIS(String modelDirectory) {
        String knowledgeBase = "http://dbpedia.org/resource/";
        algo = new NEDAlgo_HITS(modelDirectory, knowledgeBase);
    }

    public static void main(String[] args) throws IOException {
        String preAnnotatedText = "<entity>Barack Obama</entity>  meets <entity>Angela Merkel</entity>  in <entity>Berlin</entity>  to discuss a <entity>new world order</entity> ";
        // String modelDirectory = "/home/rusbeck/AGDISTIS/indexdbpedia_en";
        String modelDirectory = "/data/r.usbeck/indexdbpedia_en";
        AGDISTIS agdistis = new AGDISTIS(modelDirectory);
        HashMap<NamedEntityInText, String> results = agdistis.runDisambiguation(preAnnotatedText);
        for (NamedEntityInText namedEntity : results.keySet()) {
            String disambiguatedURL = results.get(namedEntity);
            log.info(namedEntity + " ==> " + disambiguatedURL);
        }
    }

    public HashMap<NamedEntityInText, String> runDisambiguation(String preAnnotatedText) throws IOException {
        double threshholdTrigram = 0.825;
        int maxDepth = 2;

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
        NamedEntitiesInText nes = new NamedEntitiesInText(list);
        DocumentText text = new DocumentText(preAnnotatedText);

        document.addProperty(text);
        document.addProperty(nes);

        algo.run(document, threshholdTrigram, maxDepth);
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