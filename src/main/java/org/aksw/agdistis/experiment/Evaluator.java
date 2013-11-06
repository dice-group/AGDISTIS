package org.aksw.agdistis.experiment;

import java.io.BufferedWriter;
import java.io.IOException;

import org.aksw.agdistis.algorithm.DisambiguationAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datatypeshelper.utils.corpus.Corpus;
import datatypeshelper.utils.doc.Document;
import datatypeshelper.utils.doc.DocumentText;
import datatypeshelper.utils.doc.ner.NamedEntitiesInText;
import datatypeshelper.utils.doc.ner.NamedEntityInText;

public class Evaluator {
	private Logger log = LoggerFactory.getLogger(Evaluator.class);
	private Corpus corpus;
	private String languageTag;
	private DisambiguationAlgorithm algo;
	private double threshholdTrigram;

	private double fmeasure;
	private double accuracy;

	public Evaluator(String languageTag, Corpus corpus, DisambiguationAlgorithm algo) {
		this.corpus = corpus;
		this.languageTag = languageTag;
		this.algo = algo;
	}

	public void fmeasure() {
		double tp = 0, fp = 0, fn = 0, tn = 0;
		int documentId = 0;
		for (Document document : corpus) {
			try {
				System.gc();
				log.debug("Text: " + documentId);
				if (0 < document.getProperty(DocumentText.class).getText().length()) {
					algo.run(document);
					NamedEntitiesInText namedEntities = document.getProperty(NamedEntitiesInText.class);
					for (NamedEntityInText namedEntity : namedEntities) {
						if (namedEntity.getLength() > 2) {
							String correctVotingURL = namedEntity.getNamedEntityUri();
							// TODO fix that if NIF is used
							if (correctVotingURL.startsWith("rln:"))
								correctVotingURL = correctVotingURL.replace("rln:", "http://rdflivenews.aksw.org/resource/");
							if (correctVotingURL.startsWith("dbpr:"))
								correctVotingURL = correctVotingURL.replace("dbpr:", "http://dbpedia.org/resource/");
							if (correctVotingURL.startsWith("http://de.dbpedia.org/resource/")) {
								correctVotingURL = "http://aksw.org/notInWiki";
							}
							correctVotingURL = algo.getRedirect(correctVotingURL);
							String disambiguatedURL = algo.getRedirect(algo.findResult(namedEntity));
							if (correctVotingURL.equals(disambiguatedURL)) {
								tp++;
								log.debug("\t tp: " + correctVotingURL + " -> " + disambiguatedURL);
							} else if (correctVotingURL.startsWith("http://dbpedia.org/resource/") && disambiguatedURL == null) {
								fn++;
								log.debug("\t fn: " + correctVotingURL + " -> " + disambiguatedURL);
							} else if ((correctVotingURL.startsWith("http://aksw.org/notInWiki") || correctVotingURL.startsWith("http://rdflivenews.aksw.org/resource/")) && disambiguatedURL == null) {
								tn++;
								log.debug("\t tn: " + correctVotingURL + " -> " + disambiguatedURL);
							} else if (correctVotingURL.startsWith("http://dbpedia.org/resource/") && disambiguatedURL.startsWith("http://dbpedia.org/resource/") && !(correctVotingURL.equals(disambiguatedURL))) {
								fp++;
								log.debug("\t fp: " + correctVotingURL + " -> " + disambiguatedURL);
							} else if ((correctVotingURL.startsWith("http://aksw.org/notInWiki") || correctVotingURL.startsWith("http://rdflivenews.aksw.org/resource/")) && disambiguatedURL.startsWith("http://dbpedia.org/resource/")) {
								fp++;
								log.debug("\t fp: " + correctVotingURL + " -> " + disambiguatedURL);
							} else {
								log.error("STRANGE: " + correctVotingURL + " -> " + disambiguatedURL);
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
		fmeasure = 2 * ((precision * recall) / (precision + recall));
		log.error("NED f1: " + fmeasure + " \t" + threshholdTrigram);

	}

	public void accuracy() {
		double t = 0, n = 0;
		int documentId = 0;
		for (Document document : corpus) {
			System.gc();
			try {
				System.gc();
				log.info("Text: " + documentId);
				if (0 < document.getProperty(DocumentText.class).getText().length()) {
					algo.run(document);
					NamedEntitiesInText namedEntities = document.getProperty(NamedEntitiesInText.class);
					for (NamedEntityInText namedEntity : namedEntities) {
						if (namedEntity.getLength() > 2) {
							String correctVotingURL = namedEntity.getNamedEntityUri();
							if (correctVotingURL.startsWith("rln:"))
								correctVotingURL = correctVotingURL.replace("rln:", "http://rdflivenews.aksw.org/resource/");
							if (correctVotingURL.startsWith("dbpr:"))
								correctVotingURL = correctVotingURL.replace("dbpr:", "http://dbpedia.org/resource/");
							correctVotingURL = algo.getRedirect(correctVotingURL);
							String disambiguatedURL = algo.getRedirect(algo.findResult(namedEntity));
							if (correctVotingURL != null) {
								if (correctVotingURL.equals(disambiguatedURL)) {
									t++;
									log.info("\t Disambiguated: " + correctVotingURL + " -> " + disambiguatedURL);
								} else if (languageTag.equals("en")) {
									if (correctVotingURL.equals("http://aksw.org/notInWiki") || correctVotingURL.startsWith("http://rdflivenews.aksw.org/resource/") || correctVotingURL.startsWith("http://de.dbpedia.org/")) {
										log.info("\t Closed World Assumption: " + correctVotingURL + " -> " + disambiguatedURL);
									} else {
										n++;
										log.info("\t Not disambiguated: " + correctVotingURL + " -> " + disambiguatedURL);
									}
								} else if (languageTag.equals("de")) {
									if (correctVotingURL.equals("http://aksw.org/notInWiki") || correctVotingURL.startsWith("http://rdflivenews.aksw.org/resource/")) {
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
			} catch (Exception e) {
				log.error("Cound not process doc: " + documentId);
				log.error(e.getLocalizedMessage());
			}
		}
		accuracy = t / (t + n);
		log.error("NED acc: " + accuracy + " \t " + threshholdTrigram);

	}

	public void writeAccuracyToFile(BufferedWriter bw) {
		try {
			bw.write("NED acc: " + accuracy + "\t" + threshholdTrigram + "\n");
			bw.flush();
		} catch (IOException e) {
			log.error(e.getLocalizedMessage());
		}
	}

	public void writeFmeasureToFile(BufferedWriter bw) {
		try {
			bw.write("NED f1: " + fmeasure + "\t" + threshholdTrigram + "\n");
			bw.flush();
		} catch (IOException e) {
			log.error(e.getLocalizedMessage());
		}
	}

	public void setThreshholdTrigram(double threshholdTrigram) {
		this.threshholdTrigram = threshholdTrigram;
	}

}
