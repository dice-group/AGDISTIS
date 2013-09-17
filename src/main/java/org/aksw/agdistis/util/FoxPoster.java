package org.aksw.agdistis.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.openrdf.model.Statement;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.rio.ntriples.NTriplesParser;
import org.openrdf.rio.turtle.TurtleParser;

import com.unister.semweb.ned.QRToolNED.datatypes.Candidate;
import com.unister.semweb.ned.QRToolNED.datatypes.Label;

public class FoxPoster {

    private String request = "http://139.18.2.164:4444/api";
    private String outputFormat = "turtle";// turtle | rdf
    private String taskType = "ner";// ke | ner | keandner| all
    private String nif = "false";// true | false NLP Interchange Format (NIF)
    private String inputType = "text";// url | text
    private String baseURI = "";
    private RDFParser parser;

    public FoxPoster(String outputFormat, String taskType, String nif, String inputType) throws RDFParseException {
        super();
        this.outputFormat = outputFormat;
        this.taskType = taskType;
        this.nif = nif;
        this.inputType = inputType;
        if (outputFormat.equals("turtle")) {
            parser = new TurtleParser();
        } else if (outputFormat.equals("rdf")) {
            parser = new NTriplesParser();
        } else {
            throw new RDFParseException("Unknown parser type!");
        }
    }

    public List<Label> getLabelsFromRDF(String foxOutput) throws RDFParseException, RDFHandlerException, IOException {
        StatementCollector statementCollector = new StatementCollector();
        parser.setRDFHandler(statementCollector);
        parser.setVerifyData(false);
        parser.setStopAtFirstError(false);
        parser.parse(new StringReader(foxOutput), baseURI);

        Iterator<Statement> iter = statementCollector.getStatements().iterator();
        HashMap<String, ArrayList<Integer>> RDFNodeIdToIntervals = new HashMap<String, ArrayList<Integer>>();
        HashMap<String, String> RDFNodeIdToLabel = new HashMap<String, String>();
        HashMap<String, String> RDFNodeIdToCandidate = new HashMap<String, String>();
        while (iter.hasNext()) {
            Statement statement = iter.next();
            String subject = statement.getSubject().stringValue();
            String predicate = statement.getPredicate().stringValue();
            String object = statement.getObject().stringValue();
            if (predicate.equals("http://ns.aksw.org/scms/endIndex")
                    || predicate.equals("http://ns.aksw.org/scms/beginIndex")) {
                if (RDFNodeIdToIntervals.containsKey(subject)) {
                    RDFNodeIdToIntervals.get(subject).add(Integer.valueOf(object) - 1);
                } else {
                    RDFNodeIdToIntervals.put(subject, new ArrayList<Integer>());
                    RDFNodeIdToIntervals.get(subject).add(Integer.valueOf(object) - 1);
                }
            } else if (predicate.equals("http://www.w3.org/2000/10/annotation-ns#body")) {
                RDFNodeIdToLabel.put(subject, object);
                // } else if (predicate.equals("http://ns.aksw.org/scms/means")) {
                // RDFNodeIdToCandidate.put(subject, object);
            }
        }

        List<Label> list = buildLabelList(RDFNodeIdToIntervals, RDFNodeIdToLabel, RDFNodeIdToCandidate);
        return list;
    }

    private List<Label> buildLabelList(HashMap<String, ArrayList<Integer>> RDFNodeIdToIntervals,
            HashMap<String, String> RDFNodeIdToLabel, HashMap<String, String> RDFNodeIdToCandidate) {
        List<Label> list = new ArrayList<Label>();
        Candidate candidate;
        for (String RDFNodeId : RDFNodeIdToLabel.keySet()) {
            String label = RDFNodeIdToLabel.get(RDFNodeId);
            List<Integer> indizes = RDFNodeIdToIntervals.get(RDFNodeId);

            if (RDFNodeIdToCandidate.containsKey(RDFNodeId)) {
                candidate = new Candidate(RDFNodeIdToCandidate.get(RDFNodeId), "", "");
            } else {
                candidate = null;
            }
            // there are annotations that have no indizes coming from fox
            if (indizes != null) {
                Collections.sort(indizes);
                for (int i = 0; i < indizes.size(); i += 2) {
                    Label tmp = new Label(label);
                    tmp.setStart(indizes.get(i));
                    tmp.setEnd(indizes.get(i + 1));
                    if (candidate != null) {
                        tmp.getCandidates().add(candidate);
                    }
                    list.add(tmp);
                }
            }
        }
        return list;
    }

    public String doTASK(String inputText) throws MalformedURLException, IOException, ProtocolException {

        String urlParameters = "type=" + inputType + "&" + "nif=" + nif + "&" + "task=" + taskType + "&" + "output="
                + outputFormat + "&" + "text='" + inputText + "'";

        URL url = new URL(request);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
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

        return sb.toString();
    }
}
