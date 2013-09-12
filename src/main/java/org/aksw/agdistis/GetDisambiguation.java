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
	private Spotlight spotlight;

	public GetDisambiguation() {
//		String modelDirectory = "/home/rusbeck/AGDISTIS/indexdbpedia_en";
		String modelDirectory = "/data/r.usbeck/indexdbpedia_en";
		agdistis = new AGDISTIS(modelDirectory);
		spotlight = new Spotlight();
	}

	@Post
	public String postText(String req) {
		log.info("Start working on Request");
		String text = req.split("&")[0].substring("text='".length(), req.split("&")[0].length() - 1);
		String type = req.split("&")[1].substring("type='".length(), req.split("&")[1].length() - 1);
		System.out.println(text);
		System.out.println(type);
		JSONArray arr = new org.json.simple.JSONArray();
		HashMap<NamedEntityInText, String> results = null;
		try {
			if (type.equals("agdistis")) {
				results = agdistis.runDisambiguation(text);
			} else if (type.equals("spotlight")) {
				results = spotlight.runDisambiguation(text);
			} else {
				return "ERROR: wrong type";
			}
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