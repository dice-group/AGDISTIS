package org.aksw.agdistis.webapp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.aksw.agdistis.algorithm.NEDAlgo_HITS;
import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.DocumentText;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.model.CandidatesScore;
import org.aksw.agdistis.util.NIFParser;
import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.MeaningSpan;
import org.aksw.gerbil.transfer.nif.Span;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentCreator;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentParser;
import org.aksw.gerbil.transfer.nif.data.NamedEntity;
import org.apache.commons.io.IOUtils;
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
//    private NEDAlgo_HITS agdistis;
    private TurtleNIFDocumentParser parser = new TurtleNIFDocumentParser();
    private TurtleNIFDocumentCreator creator = new TurtleNIFDocumentCreator();
    private NIFParser nifParser = new NIFParser();

//    public GetDisambiguation() {
//        try {
//            agdistis = new NEDAlgo_HITS();
//        } catch (IOException e) {
//            log.error("Can not load index due to either wrong properties in agdistis.properties or missing index at location", e);
//            System.exit(0);
//        }
//    }
    @SuppressWarnings("unchecked")
    @Post
    public String postText(Representation entity) throws IOException, Exception {
        NEDAlgo_HITS agdistis = null;
        log.info("Start working on Request for AGDISTIS");
        String result = "";
        InputStream input = entity.getStream();
        byte[] byteArray = IOUtils.toByteArray(input);
        InputStream input1 = new ByteArrayInputStream(byteArray);
        InputStream input2 = new ByteArrayInputStream(byteArray);

        try {
            agdistis = new NEDAlgo_HITS();
        } catch (IOException e) {
            log.error("Can not load index due to either wrong properties in agdistis.properties or missing index at location", e);
            System.exit(0);
        }
        result = NIFGerbil(input1, agdistis);        //This part is created to work along with GERBIL, because GERBIL only sends the NIF files without taking care of more than one parameter. So, GERBIL is not capable to send the nif in the text parameter making AGDISTIS?type=nif&text= not work.  

        if (!(result.equals(""))) {
            return result;
        } else {

            String string = IOUtils.toString(input2);
            // Parse the given representation and retrieve data
            Form form = new Form(string);
            String text = form.getFirstValue("text");
            String type = form.getFirstValue("type");
            log.info("text: " + text);
            log.info("type: " + type);

            evaluationOptionalParameters(form, agdistis);
            if (type == null) {
                type = "agdistis";
            }

            if (type.equals("agdistis")) {
                return standardAG(text, agdistis, type);             //This type is the standard and in case the user doesn't send the type parameter, it is considered as the main one(e.g AGDISTIS?type=agdistis&text=<entity>Barack Obama</entity>). 

            } else if (type.equals("nif")) {
                return NIFType(text, agdistis);                //This type is for AGDISTIS works beyond the GERBIL, this part is in case of user wants to check just a certain NIF file(e.g AGDISTIS?type=nif&text=@prefix....)
   
            } else if (type.equals("candidates")) {
                return candidateType(text, agdistis, type); //Here is to let us know about all candidates for each mention and its respective HITS/PageRank score. 
            } else {
                return "ERROR";
            }
        }

    }

    private void evaluationOptionalParameters(Form form, NEDAlgo_HITS agdistis) {
        String similarityThreshold = form.getFirstValue("similarity");
        String explorationDepth = form.getFirstValue("depth");
        String heuristicExpansion = form.getFirstValue("heuristic");

        log.info("similarityThreshold: " + similarityThreshold);
        log.info("explorationDepth: " + explorationDepth);
        log.info("heuristicExpansion: " + heuristicExpansion);

        if (similarityThreshold != null) {
            agdistis.setThreshholdTrigram(Double.valueOf(similarityThreshold));
        } else {
            agdistis.setThreshholdTrigram(0.82);
        }

        if (explorationDepth != null) {
            agdistis.setMaxDepth(Integer.valueOf(explorationDepth));
        } else {
            agdistis.setMaxDepth(2);
        }

        if (heuristicExpansion != null) {
            agdistis.setHeuristicExpansionOn(Boolean.valueOf(heuristicExpansion));
        } else {
            agdistis.setHeuristicExpansionOn(true);
        }
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

        document.addText(text);
        document.addNamedEntitiesInText(nes);
        return document;
    }

    public static HashMap<NamedEntityInText, String> results(Document document, NEDAlgo_HITS algo, String type) {
        algo.run(document, type);
        NamedEntitiesInText namedEntities = document.getNamedEntitiesInText();
        HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
        for (NamedEntityInText namedEntity : namedEntities) {
            String disambiguatedURL = algo.findResult(namedEntity);
            results.put(namedEntity, disambiguatedURL);
        }
        return results;
    }

    //To make the type parameter as "candidates" works
    private static HashMap<NamedEntityInText, ArrayList<CandidatesScore>> resultsCandidates(Document document, NEDAlgo_HITS algo, String type) {
        algo.run(document, type);
        NamedEntitiesInText namedEntities = document.getNamedEntitiesInText();
        HashMap<NamedEntityInText, ArrayList<CandidatesScore>> results = new HashMap<NamedEntityInText, ArrayList<CandidatesScore>>();
        for (NamedEntityInText namedEntity : namedEntities) {
            results.put(namedEntity, algo.findCandidates(namedEntity));
        }
        return results;
    }

    public String NIFGerbil(InputStream input, NEDAlgo_HITS agdistis) throws IOException {
        org.aksw.gerbil.transfer.nif.Document document;
        String nifDocument = "";
        String textWithMentions = "";
        List<MeaningSpan> annotations = new ArrayList<>();
        HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
        try {
            document = parser.getDocumentFromNIFStream(input);
                log.info("NIF file coming from GERBIL");
                textWithMentions = nifParser.createTextWithMentions(document.getText(), document.getMarkings(Span.class));
                Document d = textToDocument(textWithMentions);
                results = results(d, agdistis, "agdistis");
                for (NamedEntityInText namedEntity : results.keySet()) {
                    String disambiguatedURL = results.get(namedEntity);

                    if (disambiguatedURL == null) {
                        annotations.add(new NamedEntity((int) namedEntity.getStartPos(), (int) namedEntity.getLength(), new HashSet<String>()));
                    } else {
                        annotations.add(new NamedEntity((int) namedEntity.getStartPos(), (int) namedEntity.getLength(), URLDecoder.decode(disambiguatedURL, "UTF-8")));
                    }
                }
                document.setMarkings(new ArrayList<Marking>(annotations));
                log.debug("Result: " + document.toString());
                nifDocument = creator.getDocumentAsNIFString(document);
                log.debug(nifDocument);

        } catch (Exception e) {
            log.error("Exception while reading request.", e);
            return "";
        }

        return nifDocument;
    }

    public String standardAG(String text, NEDAlgo_HITS agdistis, String type) {
        HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
        JSONArray arr = new org.json.simple.JSONArray();

        Document d = textToDocument(text);
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
        log.info("\t" + arr.toString());
        log.info("Finished Request");
        return arr.toString();

    }

    public String NIFType(String text, NEDAlgo_HITS agdistis) throws IOException {
        org.aksw.gerbil.transfer.nif.Document document = null;
        String nifDocument = "";
        NIFParser nifParser = new NIFParser();
        String textWithMentions = "";
        List<MeaningSpan> annotations = new ArrayList<>();
        HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();

        try {
            document = parser.getDocumentFromNIFString(text);
            log.debug("Request: " + document.toString());
            textWithMentions = nifParser.createTextWithMentions(document.getText(), document.getMarkings(Span.class));
            Document d = textToDocument(textWithMentions);
            results = results(d, agdistis, "agdistis");
            for (NamedEntityInText namedEntity : results.keySet()) {
                String disambiguatedURL = results.get(namedEntity);

                if (disambiguatedURL == null) {
                    annotations.add(new NamedEntity((int) namedEntity.getStartPos(), (int) namedEntity.getLength(), new HashSet<String>()));
                } else {
                    annotations.add(new NamedEntity((int) namedEntity.getStartPos(), (int) namedEntity.getLength(), URLDecoder.decode(disambiguatedURL, "UTF-8")));
                }
            }
            document.setMarkings(new ArrayList<Marking>(annotations));
            log.debug("Result: " + document.toString());
            nifDocument = creator.getDocumentAsNIFString(document);
        } catch (Exception e) {
            log.error("Exception while reading request.", e);
            return "";
        }
        return nifDocument;
    }

    public String candidateType(String text, NEDAlgo_HITS agdistis, String type) {
        JSONArray arr = new org.json.simple.JSONArray();
        HashMap<NamedEntityInText, ArrayList<CandidatesScore>> resultsScore = new HashMap<NamedEntityInText, ArrayList<CandidatesScore>>();
        Document d = textToDocument(text);

        resultsScore = resultsCandidates(d, agdistis, type);
        for (NamedEntityInText namedEntity : resultsScore.keySet()) {
            ArrayList<CandidatesScore> candidates = resultsScore.get(namedEntity);
            JSONObject obj = new JSONObject();
            obj.put("namedEntity", namedEntity.getLabel());
            obj.put("Candidates", candidates.toString());
            arr.add(obj);
        }

        log.info("\t" + arr.toString());
        log.info("Finished Request");
        return arr.toString();

    }
}
