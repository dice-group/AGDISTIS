package org.aksw.agdistis.experiment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.aksw.agdistis.util.SpotlightPoster;
import org.slf4j.LoggerFactory;

import datatypeshelper.io.xml.CorpusXmlReader;
import datatypeshelper.utils.corpus.Corpus;
import datatypeshelper.utils.doc.Document;
import datatypeshelper.utils.doc.DocumentText;
import datatypeshelper.utils.doc.ner.NamedEntitiesInText;
import datatypeshelper.utils.doc.ner.NamedEntityInText;

public class SpotlightCompare {
	private static org.slf4j.Logger log = LoggerFactory.getLogger(SpotlightCompare.class);

	public static void main(String[] args) throws Exception {
		SpotlightPoster spot = new SpotlightPoster();
		for (String INPUT_FILE : new String[] { "reuters.xml" }) {// "AIDACorpus.xml",
																		// "reuters.xml",
																		// "500newsgoldstandard.xml"
			CorpusXmlReader reader = new CorpusXmlReader(new File(INPUT_FILE));
			Corpus corpus = reader.getCorpus();
			fmeasure("en", spot, corpus, INPUT_FILE);

		}
	}

	private static void fmeasure(String languageTag, SpotlightPoster spot, Corpus corpus, String iNPUT_FILE) throws IOException {
		double tp = 0, fp = 0, fn = 0, tn = 0;
		int documentId = 0;
		BufferedWriter bw = new BufferedWriter(new FileWriter("AIDA_" + iNPUT_FILE + ".txt", true));
		bw.write("input: " + iNPUT_FILE + "\n");
		for (Document document : corpus) {
			try {
				System.gc();
				spot.doTASK(document);

				NamedEntitiesInText namedEntities = document.getProperty(NamedEntitiesInText.class);
				log.info("Text: " + documentId);
				if (0 < document.getProperty(DocumentText.class).getText().length()) {
					// algo.run(document, threshholdTrigram); --> used for algo
					// without graph techniques
					for (NamedEntityInText namedEntity : namedEntities) {
						if (namedEntity.getLength() > 2) {
							String correctVotingURL = namedEntity.getNamedEntityUri();
							if (correctVotingURL.startsWith("rln:"))
								correctVotingURL = correctVotingURL.replace("rln:", "http://rdflivenews.aksw.org/resource/");
							if (correctVotingURL.startsWith("dbpr:"))
								correctVotingURL = correctVotingURL.replace("dbpr:", "http://dbpedia.org/resource/");
							String disambiguatedURL = spot.findResult(namedEntity.getStartPos());

							if (correctVotingURL.startsWith("http://de.dbpedia.org/resource/")) {
								correctVotingURL = "http://aksw.org/notInWiki";
							}
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
		}
		double precision = tp / (tp + fp);
		double recall = tp / (tp + fn);
		log.error("NED f1: " + (2 * ((precision * recall) / (precision + recall))) + " \t " + precision + "\t" + recall);
		bw.write("NED f1: " + (2 * ((precision * recall) / (precision + recall))) + " \t " + precision + "\t" + recall);
		bw.flush();
		bw.close();
	}
}