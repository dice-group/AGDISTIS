/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.agdistis.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.aksw.agdistis.algorithm.NEDAlgo_HITS;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import static org.aksw.agdistis.webapp.GetDisambiguation.textToDocument;

import org.aksw.gerbil.transfer.nif.MeaningSpan;
import org.aksw.gerbil.transfer.nif.Span;
import org.aksw.gerbil.transfer.nif.data.NamedEntity;
import org.aksw.gerbil.transfer.nif.data.StartPosBasedComparator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.aksw.gerbil.transfer.nif.Document;

/**
 *
 * @author diegomoussallem
 */
public class NIFParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(NIFParser.class);
    protected JSONParser jsonParser = new JSONParser();
    private NEDAlgo_HITS agdistis;
    
    public NIFParser() {
        try {
            agdistis = new NEDAlgo_HITS();
        } catch (IOException e) {
            LOGGER.error("Can not load index due to either wrong properties in agdistis.properties or missing index at location", e);
            System.exit(0);
        }
    }

    public List<MeaningSpan> convertNIF(Document document) throws IOException {
        String textWithMentions = createTextWithMentions(document.getText(), document.getMarkings(Span.class));
        return getAnnotations(textWithMentions);
    }

    static String createTextWithMentions(String text, List<Span> mentions) {
        // Example: 'The <entity>University of Leipzig</entity> in
        // <entity>Barack Obama</entity>.'

        Collections.sort(mentions, new StartPosBasedComparator());

        StringBuilder textBuilder = new StringBuilder();
        int lastPos = 0;
        for (int i = 0; i < mentions.size(); i++) {
            Span span = mentions.get(i);

            int begin = span.getStartPosition();
            int end = begin + span.getLength();

            if (begin < lastPos) {
                // we have two overlapping mentions --> take the larger one
                Span prev = mentions.get(i - 1);
                LOGGER.warn("\"{}\" at pos {} overlaps with \"{}\" at pos {}",
                        text.substring(span.getStartPosition(), span.getStartPosition() + span.getLength()),
                        span.getStartPosition(),
                        text.substring(prev.getStartPosition(), prev.getStartPosition() + prev.getLength()),
                        prev.getStartPosition());
                if (span.getLength() > prev.getLength()) {
                    // current is larger --> replace previous with current
                    textBuilder.delete(textBuilder.length() - prev.getLength(), textBuilder.length());
                    lastPos -= prev.getLength();
                } else // previous is larger or equal --> skip current
                {
                    continue;
                }
            }
            String before = text.substring(lastPos, begin);
            String label = text.substring(begin, end);
            lastPos = end;
            textBuilder.append(before).append("<entity>" + label + "</entity>");
        }

        String lastSnippet = text.substring(lastPos, text.length());
        textBuilder.append(lastSnippet);

        return textBuilder.toString();
    }

    public List<MeaningSpan> getAnnotations(String textWithMentions) throws IOException {
        HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
        String type = "agdistis";
        org.aksw.agdistis.datatypes.Document d = textToDocument(textWithMentions);
        JSONArray arr = new org.json.simple.JSONArray();
        List<MeaningSpan> annotations = new ArrayList<>();
        results = results(d, agdistis, type);

        for (NamedEntityInText namedEntity : results.keySet()) {
            String disambiguatedURL = results.get(namedEntity);
            JSONObject obj = new JSONObject();
            obj.put("namedEntity", namedEntity.getLabel());
            obj.put("start", namedEntity.getStartPos());
            obj.put("offset", namedEntity.getLength());
            obj.put("disambiguatedURL", disambiguatedURL);
            arr.add(obj);
        }
        LOGGER.info("\t" + arr.toString());
        LOGGER.info("Finished Request");
        InputStream is = new ByteArrayInputStream(arr.toString().getBytes("UTF-8"));
        try {
            annotations = parseJsonStream(is);
        } catch (Exception e) {
            LOGGER.error("Couldn't parse the response.", e);
        }
        return annotations;
    }

    private List<MeaningSpan> parseJsonStream(InputStream in) throws IOException, ParseException {
        List<MeaningSpan> annotations = new ArrayList<>();

        JSONArray namedEntities = (JSONArray) this.jsonParser.parse(new InputStreamReader(in, "UTF-8"));
        JSONObject namedEntity;
        String url;
        long start, length;
        for (Object obj : namedEntities) {
            namedEntity = (JSONObject) obj;

            start = (long) namedEntity.get("start");
            length = (long) namedEntity.get("offset");

            url = (String) namedEntity.get("disambiguatedURL");
            if (url == null) {
                annotations.add(new NamedEntity((int) start, (int) length, new HashSet<String>()));
            } else {
                annotations.add(new NamedEntity((int) start, (int) length, URLDecoder.decode(url, "UTF-8")));
            }
        }

        return annotations;
    }

    private static HashMap<NamedEntityInText, String> results(org.aksw.agdistis.datatypes.Document document, NEDAlgo_HITS algo, String type) {
        algo.run(document, type);
        NamedEntitiesInText namedEntities = document.getNamedEntitiesInText();
        HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
        for (NamedEntityInText namedEntity : namedEntities) {
            String disambiguatedURL = algo.findResult(namedEntity);
            results.put(namedEntity, disambiguatedURL);
        }
        return results;
    }
}
