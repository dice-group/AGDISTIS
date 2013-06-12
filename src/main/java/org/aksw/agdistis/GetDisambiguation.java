package org.aksw.agdistis;

import java.io.IOException;
import java.util.HashMap;

import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unister.semweb.topicmodeling.utils.doc.ner.NamedEntityInText;

public class GetDisambiguation extends ServerResource {
    private static Logger log = LoggerFactory.getLogger(GetDisambiguation.class);
    private AGDISTIS agdistis;

    public GetDisambiguation() {
        String modelDirectory = "model/indexdbpedia_en";
        agdistis = new AGDISTIS(modelDirectory);
    }

    @Post
    public String postText(String text) {
        log.info("Start working on Request");
        // String preAnnotatedText =
        // "[[Barack Obama]] meets [[Angela Merkel]] in [[Berlin]] to discuss a [[new world order]]";

        StringBuffer sb = new StringBuffer();
        try {
            HashMap<NamedEntityInText, String> results = agdistis.runDisambiguation(text);

            for (NamedEntityInText namedEntity : results.keySet()) {
                String disambiguatedURL = results.get(namedEntity);
                sb.append(namedEntity + " ==> " + disambiguatedURL + "\n");
                log.info("\t" + namedEntity + " ==> " + disambiguatedURL);
            }
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
        }
        log.info("Finished Request");
        return sb.toString();

    }
}