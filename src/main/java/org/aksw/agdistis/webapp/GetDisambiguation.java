package org.aksw.agdistis.webapp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  private final TurtleNIFDocumentParser parser = new TurtleNIFDocumentParser();
  private final TurtleNIFDocumentCreator creator = new TurtleNIFDocumentCreator();
  private final NIFParser nifParser = new NIFParser();

  @Post
  public String postText(final Representation entity) throws IOException, Exception {
    NEDAlgo_HITS agdistis = null;
    try {
      final String cfg = "config/agdistis.properties";
      agdistis = new NEDAlgo_HITS(cfg);
    } catch (final IOException e) {
      log.error(
          "Can not load index due to either wrong properties in agdistis.properties or missing index at location",
          e);
      System.exit(0);
    }
    log.info("Start working on Request for AGDISTIS");
    String result = "";
    String text = "";
    String type = "";
    final InputStream input = entity.getStream();
    // here the inputStream is duplicated due to it can be read only once.
    // Therefore, we do it for checking if the input is from gerbil or not.
    final byte[] byteArray = IOUtils.toByteArray(input);
    final InputStream input1 = new ByteArrayInputStream(byteArray);
    final InputStream input2 = new ByteArrayInputStream(byteArray);

    final String string = IOUtils.toString(input1);
    // Parse the given representation and retrieve data
    final Form form = new Form(string);
    text = form.getFirstValue("text");
    type = form.getFirstValue("type");
    log.info("text: " + text);
    log.info("type: " + type);

    if (text == null) {
      result = NIFGerbil(input2, agdistis); // This part is created to
                                            // work
      // along with GERBIL, because
      // GERBIL only sends the NIF
      // files without taking care of
      // more than one parameter. So,
      // GERBIL is not capable to send
      // the nif in the text parameter
      // making
      // AGDISTIS?type=nif&text= not
      // work.
      return result;
    }
    if (type == null) {
      type = "agdistis";
    }

    if (type.equals("agdistis")) {
      return standardAG(text, agdistis); // This type is the standard
                                         // and in case the user
                                         // doesn't send the type
                                         // parameter, it is
                                         // considered as the main
                                         // one(e.g
                                         // AGDISTIS?type=agdistis&text=<entity>Barack
                                         // Obama</entity>).

    } else if (type.equals("nif")) {
      return NIFType(text, agdistis); // This type is for AGDISTIS
                                      // works beyond the GERBIL, this
                                      // part is in case of user wants
                                      // to check just a certain NIF
                                      // file(e.g
                                      // AGDISTIS?type=nif&text=@prefix....)

    } else if (type.equals("candidates")) {
      return candidateType(text, agdistis); // Here is to let us know
                                            // about all candidates
                                            // for each mention and
                                            // its respective
                                            // HITS/PageRank score.
    } else {
      return "ERROR";
    }
  }

  public static Document textToDocument(final String preAnnotatedText) {
    final Document document = new Document();
    final ArrayList<NamedEntityInText> list = new ArrayList<NamedEntityInText>();
    log.info("\tText: " + preAnnotatedText);
    int startpos = 0, endpos = 0;
    final StringBuilder sb = new StringBuilder();
    startpos = preAnnotatedText.indexOf("<entity>", startpos);
    while (startpos >= 0) {
      sb.append(preAnnotatedText.substring(endpos, startpos));
      startpos += 8;
      endpos = preAnnotatedText.indexOf("</entity>", startpos);
      final int newStartPos = sb.length();
      final String entityLabel = preAnnotatedText.substring(startpos, endpos);
      list.add(new NamedEntityInText(newStartPos, entityLabel.length(), entityLabel, ""));
      sb.append(entityLabel);
      endpos += 9;
      startpos = preAnnotatedText.indexOf("<entity>", startpos);
    }

    final NamedEntitiesInText nes = new NamedEntitiesInText(list);
    final DocumentText text =
        new DocumentText(preAnnotatedText.replaceAll("<entity>", "").replaceAll("</entity>", ""));

    document.addText(text);
    document.addNamedEntitiesInText(nes);
    return document;
  }

  public String NIFGerbil(final InputStream input, final NEDAlgo_HITS agdistis) throws IOException {
    org.aksw.gerbil.transfer.nif.Document document;
    String nifDocument = "";
    String textWithMentions = "";
    final List<MeaningSpan> annotations = new ArrayList<>();
    try {
      document = parser.getDocumentFromNIFStream(input);
      log.info("NIF file coming from GERBIL");
      textWithMentions =
          nifParser.createTextWithMentions(document.getText(), document.getMarkings(Span.class));
      final Document d = textToDocument(textWithMentions);
      agdistis.run(d, null);
      for (final NamedEntityInText namedEntity : d.getNamedEntitiesInText()) {
        final String disambiguatedURL = namedEntity.getNamedEntityUri();

        if (disambiguatedURL == null) {
          annotations.add(
              new NamedEntity(namedEntity.getStartPos(), namedEntity.getLength(), URLDecoder.decode(
                  "http://aksw.org/notInWiki/" + namedEntity.getSingleWordLabel(), "UTF-8")));
        } else {
          annotations.add(new NamedEntity(namedEntity.getStartPos(), namedEntity.getLength(),
              URLDecoder.decode(namedEntity.getNamedEntityUri(), "UTF-8")));
        }
      }
      document.setMarkings(new ArrayList<Marking>(annotations));
      log.debug("Result: " + document.toString());
      nifDocument = creator.getDocumentAsNIFString(document);
      log.debug(nifDocument);

    } catch (final Exception e) {
      log.error("Exception while reading request.", e);
      return "";
    }

    return nifDocument;
  }

  @SuppressWarnings("unchecked")
  public String standardAG(final String text, final NEDAlgo_HITS agdistis) {
    final JSONArray arr = new org.json.simple.JSONArray();

    final Document d = textToDocument(text);
    agdistis.run(d, null);

    for (final NamedEntityInText namedEntity : d.getNamedEntitiesInText()) {
      if (!namedEntity.getNamedEntityUri().contains("http")) {
        namedEntity.setNamedEntity("http://aksw.org/notInWiki/" + namedEntity.getSingleWordLabel());
      }
      final JSONObject obj = new JSONObject();
      obj.put("namedEntity", namedEntity.getLabel());
      obj.put("start", namedEntity.getStartPos());
      obj.put("offset", namedEntity.getLength());
      obj.put("disambiguatedURL", namedEntity.getNamedEntityUri());
      arr.add(obj);
    }
    log.info("\t" + arr.toString());
    log.info("Finished Request");
    return arr.toString();

  }

  public String NIFType(final String text, final NEDAlgo_HITS agdistis) throws IOException {
    org.aksw.gerbil.transfer.nif.Document document = null;
    String nifDocument = "";
    final NIFParser nifParser = new NIFParser();
    String textWithMentions = "";
    final List<MeaningSpan> annotations = new ArrayList<>();

    try {
      document = parser.getDocumentFromNIFString(text);
      log.debug("Request: " + document.toString());
      textWithMentions =
          nifParser.createTextWithMentions(document.getText(), document.getMarkings(Span.class));
      final Document d = textToDocument(textWithMentions);
      agdistis.run(d, null);
      for (final NamedEntityInText namedEntity : d.getNamedEntitiesInText()) {
        final String disambiguatedURL = namedEntity.getNamedEntityUri();

        if (disambiguatedURL == null) {
          annotations.add(
              new NamedEntity(namedEntity.getStartPos(), namedEntity.getLength(), URLDecoder.decode(
                  "http://aksw.org/notInWiki/" + namedEntity.getSingleWordLabel(), "UTF-8")));
        } else {
          annotations.add(new NamedEntity(namedEntity.getStartPos(), namedEntity.getLength(),
              URLDecoder.decode(disambiguatedURL, "UTF-8")));
        }
      }
      document.setMarkings(new ArrayList<Marking>(annotations));
      log.debug("Result: " + document.toString());
      nifDocument = creator.getDocumentAsNIFString(document);
    } catch (final Exception e) {
      log.error("Exception while reading request.", e);
      return "";
    }
    return nifDocument;
  }

  @SuppressWarnings("unchecked")
  public String candidateType(final String text, final NEDAlgo_HITS agdistis) {
    final JSONArray arr = new org.json.simple.JSONArray();
    final Document d = textToDocument(text);
    final Map<NamedEntityInText, List<CandidatesScore>> candidatesPerNE = new HashMap<>();
    agdistis.run(d, candidatesPerNE);
    for (final NamedEntityInText namedEntity : candidatesPerNE.keySet()) {
      final List<CandidatesScore> candidates = candidatesPerNE.get(namedEntity);
      final JSONObject obj = new JSONObject();
      obj.put("namedEntity", namedEntity.getLabel());
      obj.put("Candidates", candidates.toString());
      arr.add(obj);
    }

    log.info("\t" + arr.toString());
    log.info("Finished Request");
    return arr.toString();

  }
}
