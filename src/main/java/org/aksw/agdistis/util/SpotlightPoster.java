package org.aksw.agdistis.util;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import datatypeshelper.utils.doc.Document;
import datatypeshelper.utils.doc.DocumentText;
import datatypeshelper.utils.doc.ner.NamedEntitiesInText;
import datatypeshelper.utils.doc.ner.NamedEntityInText;

public class SpotlightPoster {

	HashMap<Integer, String> positionToURL;

	public SpotlightPoster() {
		positionToURL = new HashMap<Integer, String>();
	}

	public static void main(String args[]) throws IOException {

		NamedEntitiesInText nes = new NamedEntitiesInText(new NamedEntityInText(38, 8, "Lovelace"), new NamedEntityInText(62, 11, "Rob Epstein"), new NamedEntityInText(78, 16, "Jeffery Friedman"), new NamedEntityInText(101, 9, "Admission"), new NamedEntityInText(126, 10, "Paul Weitz"));

		String sentence = "Recent work includes the 2013 films ``Lovelace,'' directed by Rob Epstein and Jeffery Friedman and ``Admission,'' directed by Paul Weitz.";

		Document document = new Document();
		DocumentText text = new DocumentText(sentence);

		document.addProperty(text);
		document.addProperty(nes);

		SpotlightPoster spot = new SpotlightPoster();
		spot.doTASK(document);

	}

	public void doTASK(Document document) throws IOException {
		String text = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		text += "<annotation ";
		String textValue = document.getProperty(DocumentText.class).getStringValue().replace("&", "").replace("\"", "'");
		text += "text=\"" + textValue.replaceAll("<entity>", "").replaceAll("</entity>", "") + "\">\n";
		// text += "text=\" \">\n";
		// int off = 8;
		for (NamedEntityInText ne : document.getProperty(NamedEntitiesInText.class)) {
			String namedEntity = textValue.substring(ne.getStartPos(), ne.getEndPos());
			// text += "\t<surfaceForm name=\"" + namedEntity + "\" offset=\"" +
			// (ne.getStartPos() -off) + "\" />\n";
			text += "\t<surfaceForm name=\"" + namedEntity + "\" offset=\"" + (ne.getStartPos()) + "\" />\n";
			// System.out.println(namedEntity);
			// off+=9;
		}
		text += "</annotation>";
//		System.out.println(text);
		text = URLEncoder.encode(text, "UTF-8").replace("+", "%20");
		String urlParameters = "text=" + text + "";
		// System.out.println(urlParameters);
		// String request = "http://spotlight.dbpedia.org/rest/disambiguate";
		// String request = "http://localhost:2222/rest/disambiguate";
		String request = "http://200.131.219.34:8080/dbpedia-spotlight-de/rest/disambiguate";
		// String request = "http://de.dbpedia.org/spotlight/rest/disambiguate";
		// String request =
		// "http://ec2-54-214-114-131.us-west-2.compute.amazonaws.com:8080/rest/disambiguate";

		URL url = new URL(request);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setUseCaches(false);
		connection.setInstanceFollowRedirects(true);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Accept", "application/json");
		connection.setRequestProperty("charset", "utf-8");
		connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
		DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		connection.disconnect();
		StringBuilder sb = new StringBuilder();
		// BufferedReader reader = new BufferedReader(new
		// InputStreamReader(connection.getInputStream()));
		InputStreamReader reader = new InputStreamReader(connection.getInputStream());
		char buffer[] = new char[1024];
		int length = reader.read(buffer);
		while (length > 0) {
			while (length > 0) {
				sb.append(buffer, 0, length);
				length = reader.read(buffer);
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			length = reader.read(buffer);
		}
		wr.close();
		reader.close();
		// System.out.println(URLDecoder.decode(sb.toString(), "UTF-8"));
		parseJSON(sb.toString().replace("@URI", "URI").replace("@offset", "offset"));

	}

	private void parseJSON(String string) throws IOException {
		Reader reader = new StringReader(string);

		Gson gson = new GsonBuilder().create();
		JsonText p = gson.fromJson(reader, JsonText.class);
		for (JsonEntity ent : p.Resources) {
			// System.out.println(ent + " " +URLDecoder.decode(ent.URI,
			// "UTF-8"));
			positionToURL.put(ent.offset, URLDecoder.decode(ent.URI, "UTF-8"));
		}

		reader.close();

	}

	public String findResult(int startPos) {
		return positionToURL.get(startPos);
	}

}
