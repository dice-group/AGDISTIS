package org.aksw.agdistis.experiment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.aksw.agdistis.algorithm.DisambiguationAlgorithm;
import org.aksw.agdistis.algorithm.NEDSpotlightPoster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datatypeshelper.io.xml.CorpusXmlReader;
import datatypeshelper.utils.corpus.Corpus;

public class TextDisambiguation {
	private static Logger log = LoggerFactory.getLogger(TextDisambiguation.class);

	public static void main(String[] args) throws IOException {
		String languageTag = "en"; // de
		String dataDirectory = "/data/r.usbeck"; // "/Users/ricardousbeck";
		String nodeType = "http://dbpedia.org/resource/";// "http://yago-knowledge.org/resource/"
		String edgeType = "http://dbpedia.org/ontology/";// "http://yago-knowledge.org/resource/"

		for (String TestFile : new String[] { "datasets/reuters.xml" }) {// "500newsgoldstandard.xml"
																// "german_corpus_new.xml"
																// "AIDACorpus.xml"

			CorpusXmlReader reader = new CorpusXmlReader(new File(TestFile));
			Corpus corpus = reader.getCorpus();
			log.info("Corpus size: " + corpus.getNumberOfDocuments());

//			DisambiguationAlgorithm algo = new NEDAlgo_HITS(corpus.getNumberOfDocuments(), languageTag, dataDirectory, nodeType, edgeType);
//			DisambiguationAlgorithm algo = new AIDADisambiguator();
			DisambiguationAlgorithm algo = new NEDSpotlightPoster();
			
			for (int maxDepth = 2; maxDepth <= 2; ++maxDepth) {
//				BufferedWriter bw = new BufferedWriter(new FileWriter("Test_" + TestFile + "_" + maxDepth + "_newDataset.txt", true));
//				bw.write("input: " + TestFile + "\n");

				algo.setMaxDepth(maxDepth);
				for (double threshholdTrigram = 1; threshholdTrigram > 0.994; threshholdTrigram -= 0.01) {
					algo.setThreshholdTrigram(threshholdTrigram);
				
					Evaluator ev = new Evaluator(languageTag, corpus, algo);
					ev.setThreshholdTrigram(threshholdTrigram);
					ev.fmeasure();
					// ev.writeFmeasureToFile(bw);
					
					System.gc();
				}
//				bw.close();
			}
			algo.close();
		}
	}

}