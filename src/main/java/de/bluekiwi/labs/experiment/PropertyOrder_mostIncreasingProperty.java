package de.bluekiwi.labs.experiment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unister.semweb.topicmodeling.io.xml.CorpusXmlReader;
import com.unister.semweb.topicmodeling.utils.corpus.Corpus;
import com.unister.semweb.topicmodeling.utils.doc.Document;
import com.unister.semweb.topicmodeling.utils.doc.DocumentText;
import com.unister.semweb.topicmodeling.utils.doc.ner.NamedEntitiesInText;
import com.unister.semweb.topicmodeling.utils.doc.ner.NamedEntityInText;

import de.bluekiwi.labs.algorithm.NEDAlgo_HITS;

public class PropertyOrder_mostIncreasingProperty {
    private static Logger log = LoggerFactory.getLogger(PropertyOrder_mostIncreasingProperty.class);

    public static void main(String[] args) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter("/data/r.usbeck/Dropbox/propertyOrderIncrease_all.log"));
        String PROPERTY_FILE = "propertyOrder.txt";
        String OUTPUT_FILE = "/data/r.usbeck/Dropbox/reuters.xml";
        // String OUTPUT_FILE = "/Users/ricardousbeck/Dropbox/reuters.xml";

        // String ONTOLOGY_FILE = "/data/r.usbeck/Dropbox/ontology_properties.txt";
        // String ONTOLOGY_FILE = "/Users/ricardousbeck/Dropbox/ontology_properties.txt";

        // INIT PARAMETERS AND ALGORITHM
        CorpusXmlReader reader = new CorpusXmlReader(new File(OUTPUT_FILE));
        Corpus corpus = reader.getCorpus();
        NEDAlgo_HITS algo = new NEDAlgo_HITS(corpus.getNumberOfDocuments());
        double threshholdTrigram = 0.835;

        // CALCULATE MOST OFTEN USED PROPERTIES
        Stack<String> mostOftenProperties = loadPropertyOrder(PROPERTY_FILE);
        HashSet<String> propertiesToTest = new HashSet<String>();
        for (int i = 0; i < mostOftenProperties.size(); i++) {
            propertiesToTest.add(mostOftenProperties.pop());
        }
        ArrayList<String> coolProperties = new ArrayList<String>();

        // RUN PRE STEP
        for (Document document : corpus) {
            algo.runPreStep(document, threshholdTrigram, document.getDocumentId());
        }

        double optimizedAccuracyForDataset = 0.68;
        while (!propertiesToTest.isEmpty()) {
            double minAccuracy = optimizedAccuracyForDataset;
            System.gc();
            String bestPerformingPropertyThisIteration = null;
            // take a property from queue' and add it to superior list
            for (String property : propertiesToTest) {
                log.error("To be restricted this run: " + property + " plus other: " + coolProperties.size());
                // restricted Edges are all edges minus top properties
                HashSet<String> restrictedEdges = new HashSet<String>();
                restrictedEdges.add(property);
                restrictedEdges.addAll(coolProperties);

                // delete all edges from the graphs that are not in superior list
                // run the algorithm
                algo.restrictEdgesTo(restrictedEdges);
                double t = 0;
                double n = 0;
                for (Document document : corpus) {
                    // System.out.println("Text: " + document.getDocumentId());
                    algo.runPostStep(document, threshholdTrigram, document.getDocumentId());

                    NamedEntitiesInText namedEntities = document.getProperty(NamedEntitiesInText.class);
                    for (NamedEntityInText namedEntity : namedEntities) {
                        if (namedEntity.getLength() > 2) {
                            String correctVotingURL = namedEntity.getNamedEntityUri();
                            String disambiguatedURL = algo.findResult(namedEntity);
                            if (correctVotingURL != null) {
                                if (correctVotingURL.equals(disambiguatedURL)) {
                                    t++;
                                    log.info("\t Disambiguated: " + correctVotingURL + " -> " + disambiguatedURL);
                                } else if (correctVotingURL.equals("http://aksw.org/notInWiki") ||
                                        correctVotingURL.startsWith("http://de.dbpedia.org/")) {
                                    log.info("\t Closed World Assumption: " + correctVotingURL + " -> " + disambiguatedURL);
                                } else {
                                    n++;
                                    log.info("\t Not disambiguated: " + correctVotingURL + " -> " + disambiguatedURL);
                                }
                            } else if (disambiguatedURL == null) {
                                int start = namedEntity.getStartPos();
                                int end = namedEntity.getEndPos();
                                String label = document.getProperty(DocumentText.class).getText().substring(start, end);
                                n++;
                                log.info("\t No candidates: " + label + " -> " + correctVotingURL);
                            } else {
                                log.info("\t Strange: " + correctVotingURL + " -> " + disambiguatedURL);
                            }
                        }
                    }
                }
                // generate the maximum property and add it to the superior list
                double accuracy = t / (t + n);
                if (minAccuracy < accuracy) {
                    bestPerformingPropertyThisIteration = property;
                    minAccuracy = accuracy;
                }
            }
            // if added to superior list delete it from queue
            coolProperties.add(bestPerformingPropertyThisIteration);
            propertiesToTest.remove(bestPerformingPropertyThisIteration);
            bw.write(bestPerformingPropertyThisIteration + "\tacc:\t" + minAccuracy + "\n");
            bw.flush();
            log.error(bestPerformingPropertyThisIteration + "\tacc:\t" + minAccuracy);
        }
        bw.close();
    }

    private static Stack<String> loadPropertyOrder(String PROPERTY_FILE) {
        Stack<String> tmp = new Stack<String>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(PROPERTY_FILE));
            while (br.ready()) {
                String line = br.readLine();
                line = line.substring(69, line.length() - 1);
                tmp.push(line.split(" -> ")[0].trim());
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tmp;
    }
}