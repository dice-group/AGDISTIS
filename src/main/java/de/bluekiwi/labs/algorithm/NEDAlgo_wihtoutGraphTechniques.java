package de.bluekiwi.labs.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unister.semweb.topicmodeling.utils.doc.Document;
import com.unister.semweb.topicmodeling.utils.doc.ner.NamedEntitiesInText;
import com.unister.semweb.topicmodeling.utils.doc.ner.NamedEntityInText;

import de.bluekiwi.labs.vis.MyNode;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class NEDAlgo_wihtoutGraphTechniques {
    private Logger log = LoggerFactory.getLogger(NEDAlgo_wihtoutGraphTechniques.class);
    private HashMap<Integer, String> algorithmicResult = new HashMap<Integer, String>();
    private CandidateUtil cu;

    public NEDAlgo_wihtoutGraphTechniques(int numberOfDocuments, String languageTag, String dataDirectory, String knowledgeBase) {
        cu = new CandidateUtil(languageTag, dataDirectory, knowledgeBase);
    }

    public void run(Document document, double threshholdTrigram) {
        NamedEntitiesInText namedEntities = document.getProperty(NamedEntitiesInText.class);
        algorithmicResult = new HashMap<Integer, String>();
        DirectedSparseGraph<MyNode, String> graph = new DirectedSparseGraph<MyNode, String>();
        // 0) insert candidates into Text
        cu.insertCandidatesIntoText(graph, document, threshholdTrigram);

        // 3) store the candidate with the highest hub, highest authority ratio
        ArrayList<MyNode> orderedList = new ArrayList<MyNode>();
        orderedList.addAll(graph.getVertices());
        Collections.sort(orderedList);
        for (NamedEntityInText entity : namedEntities) {
            for (int i = 0; i < orderedList.size(); i++) {
                MyNode m = orderedList.get(i);
                // there can be one node (candidate) for two labels
                if (m.containsId(entity.getStartPos())) {
                    if (!algorithmicResult.containsKey(entity.getStartPos())) {
                        algorithmicResult.put(entity.getStartPos(), m.getCandidateURI());
                        break;
                    }
                }
            }
        }
    }

    public String findResult(NamedEntityInText namedEntity) {
        if (algorithmicResult.containsKey(namedEntity.getStartPos())) {
            log.debug("\t result  " + algorithmicResult.get(namedEntity.getStartPos()));
            return algorithmicResult.get(namedEntity.getStartPos());
        } else {
            log.debug("\t result null means that we have no candidate for this NE");
            return null;
        }
    }

    public void close() {
        cu.close();
    }

    public DirectedSparseGraph<MyNode, String>[] getAllGraphs() {
        return null;
    }

    public CandidateUtil getCu() {
        return cu;
    }
}
