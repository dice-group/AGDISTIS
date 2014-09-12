package org.aksw.agdistis.experiment;

import java.io.File;
import java.io.IOException;

import org.aksw.agdistis.algorithm.DisambiguationAlgorithm;
import org.aksw.agdistis.algorithm.NEDAlgo_HITS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datatypeshelper.io.xml.CorpusXmlReader;
import datatypeshelper.utils.corpus.Corpus;

//import org.aksw.agdistis.algorithm.NEDAIDADisambiguator;

public class TextDisambiguation {
	private static Logger log = LoggerFactory.getLogger(TextDisambiguation.class);

	public static void main(String[] args) throws IOException {
		String languageTag = "en"; // de
		File dataDirectory = new File("/data/r.usbeck/indexdbpedia_en");
		// // "/Users/ricardousbeck";
		String nodeType = "http://dbpedia.org/resource/";//
		// "http://yago-knowledge.org/resource/"
		String edgeType = "http://dbpedia.org/ontology/";//
		// "http://yago-knowledge.org/resource/"

		for (String TestFile : new String[] { "datasets/500newsgoldstandard.xml" }) {
			// "german_corpus_new.xml", "datasets/500newsgoldstandard.xml"
			// "datasets/test.xml", "datasets/AIDACorpus.xml"

			CorpusXmlReader reader = new CorpusXmlReader(new File(TestFile));
			Corpus corpus = reader.getCorpus();
			log.info("Corpus size: " + corpus.getNumberOfDocuments());

			DisambiguationAlgorithm algo = new NEDAlgo_HITS(dataDirectory, nodeType, edgeType);
			// DisambiguationAlgorithm algo = new NEDAIDADisambiguator();
			// DisambiguationAlgorithm algo = new NEDSpotlightPoster();

			// for (int maxDepth = 2; maxDepth <= 2; ++maxDepth) {
			// BufferedWriter bw = new BufferedWriter(new FileWriter("SPOTLIGHT" + TestFile.replace("datasets/", "") + "_" + "_08Jan14.txt", true));
			// bw.write("input: " + TestFile + "\n");

			// for (double threshholdTrigram = 1; threshholdTrigram > 0.8;
			// threshholdTrigram -= 0.01) {
			algo.setMaxDepth(2);
			algo.setThreshholdTrigram(0.82);

			Evaluator ev = new Evaluator(languageTag, corpus, algo);
			ev.fmeasure();
			// ev.writeFmeasureToFile(bw);

			System.gc();
			// }
			// bw.close();
			// }
			algo.close();
		}
	}

}