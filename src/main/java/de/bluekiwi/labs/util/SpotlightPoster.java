package de.bluekiwi.labs.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.unister.semweb.topicmodeling.utils.doc.Document;
import com.unister.semweb.topicmodeling.utils.doc.DocumentText;
import com.unister.semweb.topicmodeling.utils.doc.ner.NamedEntitiesInText;
import com.unister.semweb.topicmodeling.utils.doc.ner.NamedEntityInText;

public class SpotlightPoster {

    HashMap<Integer, String> positionToURL;

    public SpotlightPoster() {
        positionToURL = new HashMap<Integer, String>();
    }

    public static void main(String args[]) throws IOException {

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

        SpotlightPoster spot = new SpotlightPoster();
        spot.doTASK(document);

    }

    public void doTASK(Document document) throws IOException {
        String text = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
        text += "<annotation ";
        String textValue = document.getProperty(DocumentText.class).getStringValue().replace("&", "");
        text += "text=\"" + textValue + "\">\n";
        // text += "text=\" \">\n";
        for (NamedEntityInText ne : document.getProperty(NamedEntitiesInText.class)) {
            String namedEntity = textValue.substring(ne.getStartPos(), ne.getEndPos());
            text += "\t<surfaceForm name=\"" + namedEntity + "\" offset=\"" + ne.getStartPos() + "\" />\n";
            // System.out.println(namedEntity);
        }
        text += "</annotation>";
        text = URLEncoder.encode(text, "UTF-8").replace("+", "%20");
        System.out.println(text);
        String urlParameters = "text=" + text + "";
        String request = "http://spotlight.dbpedia.org/rest/disambiguate";
        // String request = "http://de.dbpedia.org/spotlight/rest/disambiguate";
        URL url = new URL(request);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("charset", "utf-8");
        connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
        DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        connection.disconnect();
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        while (reader.ready()) {
            sb.append(reader.readLine());
        }
        wr.close();
        reader.close();

        parseHTML(sb);
    }

    private void parseHTML(StringBuilder sb) throws UnsupportedEncodingException {
        org.jsoup.nodes.Document doc = Jsoup.parse(sb.toString());
        // Elements links = doc.select("a[href]");
        // for (Element texts : links) {
        // System.out.println(texts);
        // }
        Elements links = doc.select("div");
        for (Element texts : links) {
            int pos = 0;
            char[] data = texts.html().toCharArray();
            for (int i = 0; i < data.length; ++i) {
                if (data[i] == '<' && data[i + 1] != '/') {
                    String title = "<";
                    pos = i - 1;
                    do {
                        i++;
                        title += data[i];
                        if (data[i] == '>') {
                            break;
                        }
                    } while (true);
                    org.jsoup.nodes.Document titleHTML = Jsoup.parse(title);
                    String titleString = titleHTML.select("a").attr("title");
                    titleString = URLDecoder.decode(titleString, "UTF-8");
                    positionToURL.put(pos, titleString);
                }
                ++pos;
            }
        }
    }

    public String findResult(int startPos) {
        return positionToURL.get(startPos);
    }
}
