package de.bluekiwi.labs.experiment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unister.semweb.topicmodeling.io.xml.CorpusXmlReader;
import com.unister.semweb.topicmodeling.utils.corpus.Corpus;
import com.unister.semweb.topicmodeling.utils.doc.Document;
import com.unister.semweb.topicmodeling.utils.doc.DocumentText;
import com.unister.semweb.topicmodeling.utils.doc.ner.NamedEntitiesInText;
import com.unister.semweb.topicmodeling.utils.doc.ner.NamedEntityInText;

import de.bluekiwi.labs.algorithm.NEDAlgo_HITS;

public class TextDisambiguation_Yago {
    private static Logger log = LoggerFactory.getLogger(TextDisambiguation_Yago.class);

    public static void main(String[] args) throws IOException {
        String TestFile = "AIDACorpus.xml";// "reuters.xml";// "500newsgoldstandard.xml" "german_corpus.xml"
        String languageTag = "en"; // de
        String dataDirectory = "/data/r.usbeck";
        CorpusXmlReader reader = new CorpusXmlReader(new File(TestFile));
        Corpus corpus = reader.getCorpus();

        // DISAMBIGUATION ALGORITHM
        NEDAlgo_HITS algo = new NEDAlgo_HITS(corpus.getNumberOfDocuments(), languageTag, dataDirectory, "http://yago-knowledge.org/resource/");
        // NEDAlgo_wihtoutGraphTechniques algo = new NEDAlgo_wihtoutGraphTechniques(corpus.getNumberOfDocuments());

        for (int maxDepth = 2; maxDepth <= 3; ++maxDepth) {
            BufferedWriter bw = new BufferedWriter(new FileWriter("Test_" + TestFile + "_" + maxDepth + "_Yago.txt", true));
            bw.write("input: " + TestFile + "\n");
            for (double threshholdTrigram = 1; threshholdTrigram > 0.5; threshholdTrigram -= 0.01) {
                double t = 0, n = 0;
                int documentId = 0;
                for (Document document : corpus) {
                    System.gc();
                    log.info("Text: " + documentId);
                    // if (documentId > 11)
                    if (0 < document.getProperty(DocumentText.class).getText().length()) {
                        algo.run(document, threshholdTrigram, maxDepth);
                        // algo.run(document, threshholdTrigram); --> used for algo without graph techniques
                        NamedEntitiesInText namedEntities = document.getProperty(NamedEntitiesInText.class);
                        for (NamedEntityInText namedEntity : namedEntities) {
                            if (namedEntity.getLength() > 2) {
                                String correctVotingURL = namedEntity.getNamedEntityUri();
                                if (correctVotingURL.startsWith("rln:"))
                                    correctVotingURL = correctVotingURL.replace("rln:", "http://rdflivenews.aksw.org/resource/");
                                if (correctVotingURL.startsWith("dbpr:"))
                                    correctVotingURL = correctVotingURL.replace("dbpr:", "http://dbpedia.org/resource/");
                                // correctVotingURL = algo.getCu().mapToDbpedia(correctVotingURL);
                                String disambiguatedURL = algo.getCu().mapToDbpedia(algo.findResult(namedEntity));
                                if (correctVotingURL != null) {
                                    if (correctVotingURL.equals(disambiguatedURL)) {
                                        t++;
                                        log.info("\t Disambiguated: " + correctVotingURL + " -> " + disambiguatedURL);
                                    } else if (languageTag.equals("en")) {
                                        if (correctVotingURL.equals("http://aksw.org/notInWiki")
                                                || correctVotingURL.startsWith("http://rdflivenews.aksw.org/resource/")
                                                || correctVotingURL.startsWith("http://de.dbpedia.org/")) {
                                            log.info("\t Closed World Assumption: " + correctVotingURL + " -> " + disambiguatedURL);
                                        } else {
                                            n++;
                                            log.info("\t Not disambiguated: " + correctVotingURL + " -> " + disambiguatedURL);
                                        }
                                    } else if (languageTag.equals("de")) {
                                        if (correctVotingURL.equals("http://aksw.org/notInWiki")
                                                || correctVotingURL.startsWith("http://rdflivenews.aksw.org/resource/")) {
                                            log.info("\t Closed World Assumption: " + correctVotingURL + " -> " + disambiguatedURL);
                                        } else {
                                            n++;
                                            log.info("\t Not disambiguated: " + correctVotingURL + " -> " + disambiguatedURL);
                                        }
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
                    } else {
                        log.error("Text is empty!");
                    }
                    documentId++;
                }
                bw.write((t / (t + n)) + "\t" + threshholdTrigram + "\n");
                bw.flush();
                log.error(" NED: " + t / (t + n) + " \t " + threshholdTrigram);
            }
            bw.close();
        }
        algo.close();
    }
}