package org.aksw.agdistis.experiment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mpi.aida.AidaManager;
import mpi.aida.Disambiguator;
import mpi.aida.Preparator;
import mpi.aida.config.settings.DisambiguationSettings;
import mpi.aida.config.settings.PreparationSettings;
import mpi.aida.config.settings.disambiguation.CocktailPartyDisambiguationSettings;
import mpi.aida.config.settings.preparation.StanfordHybridPreparationSettings;
import mpi.aida.data.DisambiguationResults;
import mpi.aida.data.PreparedInput;
import mpi.aida.data.ResultEntity;
import mpi.aida.data.ResultMention;

import org.slf4j.LoggerFactory;

import datatypeshelper.io.xml.CorpusXmlReader;
import datatypeshelper.utils.corpus.Corpus;
import datatypeshelper.utils.doc.Document;
import datatypeshelper.utils.doc.DocumentText;
import datatypeshelper.utils.doc.ner.NamedEntitiesInText;
import datatypeshelper.utils.doc.ner.NamedEntityInText;

public class AIDAcompare {
	private static org.slf4j.Logger log = LoggerFactory.getLogger(AIDAcompare.class);

	public static void main(String[] args) throws Exception {
		String INPUT_FILE = "reuters.xml"; // " 500newsgoldstandard.xml";//
											// reuters.xml ,
											// german_corpus.xml
		CorpusXmlReader reader = new CorpusXmlReader(new File(INPUT_FILE));
		Corpus corpus = reader.getCorpus();
		String languageTag = "en";
		fmeasure(languageTag, corpus,INPUT_FILE);

	}

	private static String findResult(int startPos, DisambiguationResults results) {
		// Print the disambiguation results.
		for (ResultMention rm : results.getResultMentions()) {
			ResultEntity re = results.getBestEntity(rm);
			if (rm.getCharacterOffset() == startPos) {
				String wikiURL = AidaManager.getWikipediaUrl(re);
				return wikiURL.replace("http://en.wikipedia.org/wiki", "http://dbpedia.org/resource");
			}
		}
		return null;
	}

	private static String markupText(Document document) {
		List<String> textParts = new ArrayList<String>();
		List<NamedEntityInText> labels = document.getProperty(NamedEntitiesInText.class).getNamedEntities();
		Collections.sort(labels);
		String originalText = document.getProperty(DocumentText.class).getText();
		// start with the last label and add the parts of the new text beginning
		// with its end to the array
		// Note that we are expecting that the labels are sorted descending by
		// there position in the text!
		int startFormerLabel = originalText.length();
		for (NamedEntityInText entity : labels) {
			// proof if this label undercuts the last one.
			if (startFormerLabel >= entity.getEndPos()) {
				// append the text between this label and the former one
				textParts.add(originalText.substring(entity.getEndPos(), startFormerLabel));
				// append the markedup label
				String label = originalText.substring(entity.getStartPos(), entity.getEndPos());
				textParts.add("[[" + label + "]]");
				// remember the start position of this label
				startFormerLabel = entity.getStartPos();
			} else {
				log.error("Label undercuts another label. TextId: " + document.getDocumentId());
			}
		}
		textParts.add(originalText.substring(0, startFormerLabel));
		// Form the new text beginning with its end
		StringBuilder textWithMarkups = new StringBuilder();
		for (int i = textParts.size() - 1; i >= 0; --i) {
			textWithMarkups.append(textParts.get(i));
		}
		return textWithMarkups.toString();
	}

	private static void fmeasure(String languageTag, Corpus corpus, String iNPUT_FILE) throws IOException {
		double tp = 0, fp = 0, fn = 0, tn = 0;
		int documentId = 0;
		BufferedWriter bw = new BufferedWriter(new FileWriter("AIDA_" + iNPUT_FILE+".txt", true));
		bw.write("input: " + iNPUT_FILE + "\n");
		for (Document document : corpus) {
			try {
				System.gc();
				String text = markupText(document);
				// Prepare the input for disambiguation. The Stanford NER
				// will be run to identify names. Strings marked with [[ ]]
				// will also betreated as names.
				PreparationSettings prepSettings = new StanfordHybridPreparationSettings();
				Preparator p = new Preparator();
				PreparedInput input = p.prepare(text, prepSettings);
				// Disambiguate the input with the graph coherence
				// algorithm.
				DisambiguationSettings disSettings = new CocktailPartyDisambiguationSettings();
				Disambiguator d = new Disambiguator(input, disSettings);
				DisambiguationResults results = d.disambiguate();
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
							String disambiguatedURL = findResult(namedEntity.getStartPos(), results);
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
		}
		double precision = tp / (tp + fp);
		double recall = tp / (tp + fn);
		log.error("NED f1: " + (2 * ((precision * recall) / (precision + recall))) + " \t " + precision + "\t" + recall );
		bw.write("NED f1: " + (2 * ((precision * recall) / (precision + recall))) + " \t " + precision + "\t" + recall );
		bw.flush();
		bw.close();
	}
}
