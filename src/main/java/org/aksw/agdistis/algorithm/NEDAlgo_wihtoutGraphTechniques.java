package org.aksw.agdistis.algorithm;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.aksw.agdistis.graph.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datatypeshelper.utils.doc.Document;
import datatypeshelper.utils.doc.ner.NamedEntitiesInText;
import datatypeshelper.utils.doc.ner.NamedEntityInText;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class NEDAlgo_wihtoutGraphTechniques {
    private Logger log = LoggerFactory.getLogger(NEDAlgo_wihtoutGraphTechniques.class);
    private HashMap<Integer, String> algorithmicResult = new HashMap<Integer, String>();
    private CandidateUtil cu;

    public NEDAlgo_wihtoutGraphTechniques(int numberOfDocuments, String languageTag, File indexDirectory, String knowledgeBase) {
        cu = new CandidateUtil(indexDirectory,null);
    }

    public void run(Document document, double threshholdTrigram) {
        NamedEntitiesInText namedEntities = document.getProperty(NamedEntitiesInText.class);
        algorithmicResult = new HashMap<Integer, String>();
        DirectedSparseGraph<Node, String> graph = new DirectedSparseGraph<Node, String>();
        // 0) insert candidates into Text
        cu.insertCandidatesIntoText(graph, document, threshholdTrigram);

        // 3) store the candidate with the highest hub, highest authority ratio
        ArrayList<Node> orderedList = new ArrayList<Node>();
        orderedList.addAll(graph.getVertices());
        Collections.sort(orderedList);
        for (NamedEntityInText entity : namedEntities) {
            for (int i = 0; i < orderedList.size(); i++) {
                Node m = orderedList.get(i);
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

    public DirectedSparseGraph<Node, String>[] getAllGraphs() {
        return null;
    }

    public CandidateUtil getCu() {
        return cu;
    }
}
