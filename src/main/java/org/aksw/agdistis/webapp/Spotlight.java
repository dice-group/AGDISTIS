package org.aksw.agdistis.webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.aksw.agdistis.util.SpotlightPoster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datatypeshelper.utils.doc.Document;
import datatypeshelper.utils.doc.DocumentText;
import datatypeshelper.utils.doc.ner.NamedEntitiesInText;
import datatypeshelper.utils.doc.ner.NamedEntityInText;

public class Spotlight {
	private static Logger log = LoggerFactory.getLogger(Spotlight.class);
	private SpotlightPoster spotlight;

	public Spotlight() {
		spotlight = new SpotlightPoster();

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
					NamedEntityInText namedEntityInText = new NamedEntityInText(beginIndex, (endIndex - beginIndex), label);
					namedEntityInText.setStartPos(beginIndex);
					list.add(namedEntityInText);
				}
			}
		}
		NamedEntitiesInText nes = new NamedEntitiesInText(list);
		DocumentText text = new DocumentText(preAnnotatedText);
		document.addProperty(text);
		document.addProperty(nes);

		spotlight.doTASK(document);

		NamedEntitiesInText namedEntities = document.getProperty(NamedEntitiesInText.class);
		HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
		int off =8;
		for (NamedEntityInText namedEntity : namedEntities) {
			String disambiguatedURL = spotlight.findResult(namedEntity.getStartPos()-off);
			results.put(namedEntity, disambiguatedURL);
			off+=9;
		}
		return results;

	}

	public void close() {
	}
}
