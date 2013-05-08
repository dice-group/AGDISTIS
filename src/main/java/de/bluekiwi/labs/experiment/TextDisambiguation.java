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

public class TextDisambiguation {
    private static Logger log = LoggerFactory.getLogger(TextDisambiguation.class);

    public static void main(String[] args) throws IOException {
        // String OUTPUT_FILE = "/Users/ricardousbeck/Dropbox/Dissertation/ISWC13_LODNED_data/german_corpus.xml";
        String OUTPUT_FILE = "/data/r.usbeck/Dropbox/Dissertation/ISWC13_LODNED_data/german_corpus.xml";
        // String OUTPUT_FILE = "/Users/ricardousbeck/Dropbox/500newsgoldstandard.xml";
        // String OUTPUT_FILE = "/data/r.usbeck/Dropbox/500newsgoldstandard.xml";
        // String OUTPUT_FILE = "/Users/ricardousbeck/Dropbox/Dissertation/ISWC13_LODNED_data/reuters.xml";
        // String OUTPUT_FILE = "/data/r.usbeck/Dropbox/Dissertation/ISWC13_LODNED_data/reuters.xml";
        CorpusXmlReader reader = new CorpusXmlReader(new File(OUTPUT_FILE));
        Corpus corpus = reader.getCorpus();

        // DISAMBIGUATION ALGORITHM
        // NEDAlgo_Prior algo = new NEDAlgo_Prior();
        NEDAlgo_HITS algo = new NEDAlgo_HITS(corpus.getNumberOfDocuments());
        // NEDAlgo_wihtoutGraphTechniques algo = new NEDAlgo_wihtoutGraphTechniques(corpus.getNumberOfDocuments());

        for (int maxDepth = 1; maxDepth <= 3; ++maxDepth) {
            BufferedWriter bw = new BufferedWriter(new FileWriter("/data/r.usbeck/Dropbox/Dissertation/ISWC13_LODNED_data/new_german_" + maxDepth + ".txt"));
            bw.write("input: " + OUTPUT_FILE + "\n");
            for (double threshholdTrigram = 1; threshholdTrigram > 0; threshholdTrigram -= 0.01) {
                double t = 0;
                double n = 0;
                int documentId = 0;
                for (Document document : corpus) {
                    System.gc();
                    log.info("Text: " + documentId);
                    if (0 < document.getProperty(DocumentText.class).getText().length()) {
                        // TODO language dependent
                        algo.run(document, threshholdTrigram, maxDepth);
                        // algo.run(document, threshholdTrigram);
                        NamedEntitiesInText namedEntities = document.getProperty(NamedEntitiesInText.class);
                        // double tt = 0;
                        // double nn = 0;
                        for (NamedEntityInText namedEntity : namedEntities) {
                            if (namedEntity.getLength() > 2) {
                                String correctVotingURL = namedEntity.getNamedEntityUri();
                                if (correctVotingURL.startsWith("rln:"))
                                    correctVotingURL = correctVotingURL.replace("rln:", "http://rdflivenews.aksw.org/resource/");
                                if (correctVotingURL.startsWith("dbpr:"))
                                    correctVotingURL = correctVotingURL.replace("dbpr:", "http://dbpedia.org/resource/");
                                correctVotingURL = algo.getCu().redirect(correctVotingURL);
                                String disambiguatedURL = algo.findResult(namedEntity);
                                if (correctVotingURL != null) {
                                    if (correctVotingURL.equals(disambiguatedURL)) {
                                        t++;
                                        // tt++;
                                        log.info("\t Disambiguated: " + correctVotingURL + " -> " + disambiguatedURL);
                                        // TODO hack it language dependend
                                    } else if (correctVotingURL.equals("http://aksw.org/notInWiki") || correctVotingURL.startsWith("http://rdflivenews.aksw.org/resource/"))
                                    {
                                        // || correctVotingURL.startsWith("http://de.dbpedia.org/")) {
                                        log.info("\t Closed World Assumption: " + correctVotingURL + " -> " + disambiguatedURL);
                                    } else {
                                        n++;
                                        // nn++;
                                        log.info("\t Not disambiguated: " + correctVotingURL + " -> " + disambiguatedURL);
                                    }
                                } else if (disambiguatedURL == null) {
                                    int start = namedEntity.getStartPos();
                                    int end = namedEntity.getEndPos();
                                    String label = document.getProperty(DocumentText.class).getText().substring(start, end);
                                    n++;
                                    // nn++;
                                    log.info("\t No candidates: " + label + " -> " + correctVotingURL);
                                } else {
                                    log.info("\t Strange: " + correctVotingURL + " -> " + disambiguatedURL);
                                }
                            }
                        }
                        // if ((tt + nn) > 0) {
                        // bw.write(documentId + "\t" + namedEntities.getNamedEntities().size() + "\t" + tt / (tt +
                        // nn));
                        // bw.newLine();
                        // }
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