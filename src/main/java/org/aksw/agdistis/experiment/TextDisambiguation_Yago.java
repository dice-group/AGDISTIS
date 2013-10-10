package org.aksw.agdistis.experiment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.aksw.agdistis.algorithm.NEDAlgo_HITS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datatypeshelper.io.xml.CorpusXmlReader;
import datatypeshelper.utils.corpus.Corpus;
import datatypeshelper.utils.doc.Document;
import datatypeshelper.utils.doc.DocumentText;
import datatypeshelper.utils.doc.ner.NamedEntitiesInText;
import datatypeshelper.utils.doc.ner.NamedEntityInText;

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
private static void fmeasure(String languageTag, Corpus corpus, NEDAlgo_HITS algo, int maxDepth, double threshholdTrigram, BufferedWriter bw) throws IOException {
	double tp = 0, fp = 0, fn = 0, tn = 0;
	int documentId = 0;
	for (Document document : corpus) {
		long startTime = System.currentTimeMillis();
		try {
			System.gc();
			log.info("Text: " + documentId);
			if (0 < document.getProperty(DocumentText.class).getText().length()) {
				algo.run(document, threshholdTrigram, maxDepth);
				// algo.run(document, threshholdTrigram); --> used for algo
				// without graph techniques
				NamedEntitiesInText namedEntities = document.getProperty(NamedEntitiesInText.class);
				for (NamedEntityInText namedEntity : namedEntities) {
					if (namedEntity.getLength() > 2) {
						String correctVotingURL = namedEntity.getNamedEntityUri();
						if (correctVotingURL.startsWith("rln:"))
							correctVotingURL = correctVotingURL.replace("rln:", "http://rdflivenews.aksw.org/resource/");
						if (correctVotingURL.startsWith("dbpr:"))
							correctVotingURL = correctVotingURL.replace("dbpr:", "http://dbpedia.org/resource/");
						correctVotingURL = algo.getCu().redirect(correctVotingURL);
						String disambiguatedURL = algo.getCu().redirect(algo.findResult(namedEntity));
						if (correctVotingURL.startsWith("http://de.dbpedia.org/resource/")) {
							correctVotingURL = "http://aksw.org/notInWiki";
						}
						// log.info("\t\t"+correctVotingURL + " " +
						// disambiguatedURL);
						if (languageTag.equals("en")) {
							if (correctVotingURL.equals(disambiguatedURL)) {
								tp++;
								log.info("\t tp: " + correctVotingURL + " -> " + disambiguatedURL);
							} else if (correctVotingURL.startsWith("http://dbpedia.org/resource/") && disambiguatedURL == null) {
								fn++;
								log.info("\t fn: " + correctVotingURL + " -> " + disambiguatedURL);
							} else if ((correctVotingURL.equals("http://aksw.org/notInWiki") || correctVotingURL.equals("http://rdflivenews.aksw.org/resource/")) && disambiguatedURL == null) {
								tn++;
								log.info("\t tn: " + correctVotingURL + " -> " + disambiguatedURL);
							} else if (correctVotingURL.startsWith("http://dbpedia.org/resource/") && disambiguatedURL.startsWith("http://dbpedia.org/resource/") && !(correctVotingURL.equals(disambiguatedURL))) {
								fp++;
								log.info("\t fp: " + correctVotingURL + " -> " + disambiguatedURL);
							} else if ((correctVotingURL.equals("http://aksw.org/notInWiki") || correctVotingURL.equals("http://rdflivenews.aksw.org/resource/")) && disambiguatedURL.startsWith("http://dbpedia.org/resource/")) {
								fp++;
								log.info("\t fp: " + correctVotingURL + " -> " + disambiguatedURL);
							} else {
								log.error("STRANGE: " + correctVotingURL + " -> " + disambiguatedURL);
							}
						} else if (languageTag.equals("de")) {
							// TODO
						}

					}
				}
			} else {
				log.error("Text is empty!");
			}
			documentId++;
		} catch (Exception e) {
			log.error("Cound not process doc: " + documentId);
			log.error(e.getLocalizedMessage());
		}
//		bw.write("Doc:\t" + documentId + "\ttime\t" + (System.currentTimeMillis() - startTime) + "\tentities\t" + document.getProperty(NamedEntitiesInText.class).getNamedEntities().size());
//		bw.newLine();
//		bw.flush();
	}
	double precision = tp / (tp + fp);
	double recall = tp / (tp + fn);
	log.error("NED f1: " + (2 * ((precision * recall) / (precision + recall))) + " \t " + precision + "\t" + recall + "\t" + threshholdTrigram);
	bw.write("NED f1: " + (2 * ((precision * recall) / (precision + recall))) + " \t " + precision + "\t" + recall + "\t" + threshholdTrigram);
	bw.flush();
}}