package org.aksw.agdistis.webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.aksw.agdistis.algorithm.NEDAlgo_HITS;
import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.DocumentText;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetDisambiguation extends ServerResource {
	private static Logger log = LoggerFactory.getLogger(GetDisambiguation.class);
	private NEDAlgo_HITS agdistis;

	public GetDisambiguation() {
		try {
			agdistis = new NEDAlgo_HITS( );
		} catch ( IOException e) {
			log.error("Can not load index due to either wrong properties in agdistis.properties or missing index at location", e);
			System.exit(0);
		}
	}

	@SuppressWarnings("unchecked")
	@Post
	public String postText(Representation entity) {
		log.info("Start working on Request for AGDISTIS");
		// Parse the given representation and retrieve data
		Form form = new Form(entity);
		String text = form.getFirstValue("text");
		String type = form.getFirstValue("type");
		log.info("text: " + text);
		log.info("type: " + type);
		
		
		JSONArray arr = new org.json.simple.JSONArray();
		HashMap<NamedEntityInText, String> results = null;
		Document d = textToDocument(text);
		
		if (type.equals("agdistis")) {
			results = results(d, agdistis);
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
		log.info("Finished Request");
		return arr.toString();

	}

	public static Document textToDocument(String preAnnotatedText) {
		Document document = new Document();
		ArrayList<NamedEntityInText> list = new ArrayList<NamedEntityInText>();
		log.info("\tText: " + preAnnotatedText);
		int startpos = 0, endpos = 0;
		StringBuilder sb = new StringBuilder();
		startpos = preAnnotatedText.indexOf("<entity>", startpos);
		while (startpos >= 0) {
			sb.append(preAnnotatedText.substring(endpos, startpos));
			startpos += 8;
			endpos = preAnnotatedText.indexOf("</entity>", startpos);
			int newStartPos = sb.length();
			String entityLabel = preAnnotatedText.substring(startpos, endpos);
			list.add(new NamedEntityInText(newStartPos, entityLabel.length(), entityLabel));
			sb.append(entityLabel);
			endpos += 9;
			startpos = preAnnotatedText.indexOf("<entity>", startpos);
		}

		NamedEntitiesInText nes = new NamedEntitiesInText(list);
		DocumentText text = new DocumentText(preAnnotatedText.replaceAll("<entity>", "").replaceAll("</entity>", ""));

		document.addTest(text);
		document.addNamedEntitiesInText(nes);
		return document;
	}

	private static HashMap<NamedEntityInText, String> results(Document document, NEDAlgo_HITS algo) {
		algo.run(document);
		NamedEntitiesInText namedEntities = document.getNamedEntitiesInText();
		HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
		for (NamedEntityInText namedEntity : namedEntities) {
			String disambiguatedURL = algo.findResult(namedEntity);
			results.put(namedEntity, disambiguatedURL);
		}
		return results;
	}

	public void close() throws IOException {
		agdistis.close();
	}

}