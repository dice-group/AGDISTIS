package org.aksw.agdistis.fox;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

import org.aksw.agdistis.model.NamedEntity;
import org.aksw.agdistis.util.Utils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.RDFReader;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

public class Fox extends ASpotter {
    private static Logger log = LoggerFactory.getLogger(Fox.class);
    private String requestURL = "http://139.18.2.164:4444/api";
    private String outputFormat = "N-Triples";
    private String taskType = "NER";
    private String inputType = "text";

    private String doTASK(String inputText) throws MalformedURLException, IOException, ProtocolException {
        String urlParameters = "type=" + inputType;
        urlParameters += "&task=" + taskType;
        urlParameters += "&output=" + outputFormat;
        urlParameters += "&input=" + URLEncoder.encode(inputText, "UTF-8");
        log.info("to fox: {}", urlParameters);
        return requestPOST(urlParameters, requestURL);
    }

    @Override
    public List<NamedEntity> getEntities(String question) {
        List<NamedEntity> result = Lists.newLinkedList();
        try {
            String foxJSONOutput = doTASK(question);

            JSONParser parser = new JSONParser();
            JSONObject jsonArray = (JSONObject) parser.parse(foxJSONOutput);
            String output = URLDecoder.decode((String) ((JSONObject) jsonArray).get("output"), "UTF-8");
            //log.info("From Fox: {}", output);
            String baseURI = "http://dbpedia.org";
            Model model = ModelFactory.createDefaultModel();
            RDFReader r = model.getReader("N3");
            r.read(model, new StringReader(output), baseURI);
            ResIterator iter = model.listSubjects();
            while (iter.hasNext()) {
                Resource next = iter.next();
                StmtIterator statementIter = next.listProperties();
                NamedEntity ent = new NamedEntity();
               // log.debug("ANNOT");
                List<Integer> starts = Lists.newArrayList();
                List<Integer> ends = Lists.newArrayList();

                while (statementIter.hasNext()) {
                    Statement statement = statementIter.next();
                    String predicateURI = statement.getPredicate().getURI();
                    RDFNode object = statement.getObject();
                    String subject = statement.getSubject().getURI();
                   // log.debug("{} -> ({}) -> {}", new Object[] { subject, predicateURI, object });

                    if (predicateURI.equals("http://www.w3.org/2000/10/annotation-ns#body")) {
                        ent.setNamedEntity(object.asLiteral().getString());
                    } else if (predicateURI.equals("http://ns.aksw.org/scms/means")) {
                        String uri = object.asResource().getURI();
                        String encode = uri.replaceAll(",", "%2C");
                        ent.setDisambiguatedURL(encode);
                    } else if (predicateURI.equals("http://ns.aksw.org/scms/endIndex")) {
                        ends.add(object.asLiteral().getInt());
                    } else if (predicateURI.equals("http://ns.aksw.org/scms/beginIndex")) {
                        starts.add(object.asLiteral().getInt());
                    }
                    else if (predicateURI.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") && object.asResource().toString().contains("http://ns.aksw.org/scms/annotations/")) {
                        String uri = object.asResource().getURI();
                        String encode = uri.replaceAll(",", "%2C");
                        ent.setType(encode);
                    }
                }
                int[] begins = Utils.convertIntegers(ends);
                int[] endings = Utils.convertIntegers(starts);
                Arrays.sort(begins);
                Arrays.sort(endings);
                ent.setEnd(begins);
                ent.setStart(endings);
                result.add(ent);
                result = splitTheEntities(result);
            }
        } catch (IOException e) {
            log.error("Could not call FOX for NER/NED", e);
        } catch (ParseException e) {
            log.error("Could not call FOX for NER/NED", e);
        }
        return result;
    }

    private List<NamedEntity> splitTheEntities(List<NamedEntity> entities) {
        List<NamedEntity> result = Lists.newArrayList();
        for (NamedEntity n : entities) {
            if (n.getStart().length == 1) {
                n.setOffset(n.getEnd()[0] - n.getStart()[0]);
                result.add(n);
            } else {
                for (int i = 0; i < n.getStart().length; i++) {
                    NamedEntity s = new NamedEntity(n);
                    s.setStart(new int[] { n.getStart()[i] });
                    s.setEnd(new int[] { n.getEnd()[i] });
                    s.setOffset(s.getEnd()[0] - s.getStart()[0]);
                    result.add(s);
                }
            }
        }

        return result;
    }

    public static void main(String args[]) {
        ASpotter fox = new Fox();
        List<NamedEntity> entities = fox.getEntities("University of Leipzig is in Leipzig near MDR tower, near Leipzig, Leipzig and in Leipzig also is one");
        if (log.isDebugEnabled()) {
            for (NamedEntity namedEntity : entities) {
                log.debug("{}", namedEntity.toString());
                System.out.println(namedEntity.toString());
            }
        }
    }
}