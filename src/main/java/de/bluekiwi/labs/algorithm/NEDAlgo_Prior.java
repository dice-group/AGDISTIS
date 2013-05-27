package de.bluekiwi.labs.algorithm;

import java.util.HashMap;

import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unister.semweb.ned.QRToolNED.datatypes.Candidate;
import com.unister.semweb.ned.QRToolNED.datatypes.Label;
import com.unister.semweb.ned.QRToolNED.datatypes.TextWithLabels;

import de.bluekiwi.labs.util.DBPedia;

public class NEDAlgo_Prior {
    Logger log = LoggerFactory.getLogger(NEDAlgo_Prior.class);
    HashMap<Integer, String> algorithmicResult = new HashMap<Integer, String>();
    String edgeType = "http://dbpedia.org/ontology/";

    public void run(TextWithLabels text) throws RepositoryException {
        String publicEndpoint = "http://localhost:8080/bigdata-dbpedia/sparql";
        DBPedia dbpedia = new DBPedia(publicEndpoint);
        for (Label l : text.getLabels()) { // find all candidates
            int maxOutgoingNodes = -1;
            for (Candidate c : l.getCandidates()) { // store the candidate for each label with the highest apriori count
                int edgeCount = -1;
                if (c.getUrl() != null) {
                    // FILTERING candidates that are not entities or entities with user input
                    if (c.getUrl().startsWith("http://dbpedia.org/resource")) {
                        edgeCount = dbpedia.getOutgoingNodes(c.getUrl(), edgeType).size();
                    }
                    if (edgeCount > maxOutgoingNodes) {
                        maxOutgoingNodes = c.getOutgoingEdgeCount();
                        algorithmicResult.put(l.getLabelId(), c.getUrl());
                    }
                }
            }
        }
    }

    public String findDisambiguationForLabel(Label tmpLabel) {
        if (algorithmicResult.containsKey(tmpLabel.getLabelId())) {
            return algorithmicResult.get(tmpLabel.getLabelId());
        } else {
            return null;
        }
    }
}
