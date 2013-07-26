package org.aksw.agdistis;

import java.io.IOException;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unister.semweb.topicmodeling.utils.doc.ner.NamedEntityInText;

public class GetDisambiguation extends ServerResource {
    private static Logger log = LoggerFactory.getLogger(GetDisambiguation.class);
    private AGDISTIS agdistis;

    public GetDisambiguation() {
        String modelDirectory = "/home/rusbeck/AGDISTIS/indexdbpedia_en";
        agdistis = new AGDISTIS(modelDirectory);
    }

    @Post
    public String postText(String text) {
        log.info("Start working on Request");

        JSONArray arr = new org.json.simple.JSONArray();
        try {
            HashMap<NamedEntityInText, String> results = agdistis.runDisambiguation(text);

            for (NamedEntityInText namedEntity : results.keySet()) {
                String disambiguatedURL = results.get(namedEntity);
                JSONObject obj = new JSONObject();
                obj.put("namedEntity", namedEntity.getLabel());
                obj.put("start", namedEntity.getStartPos());
                obj.put("offset", namedEntity.getLength());
                obj.put("disambiguatedURL", disambiguatedURL);
                arr.add(obj);
            }
            log.info("\t" + arr.toString());

        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
        }
        log.info("Finished Request");
        return arr.toString();

    }
}