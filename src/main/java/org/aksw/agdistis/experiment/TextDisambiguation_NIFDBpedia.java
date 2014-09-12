package org.aksw.agdistis.experiment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.aksw.agdistis.algorithm.DisambiguationAlgorithm;
import org.aksw.agdistis.algorithm.NEDAlgo_HITS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datatypeshelper.io.DBpediaSpotlightNIFReader;
import datatypeshelper.utils.corpus.Corpus;

public class TextDisambiguation_NIFDBpedia {
	private static Logger log = LoggerFactory.getLogger(TextDisambiguation_NIFDBpedia.class);

	public static void main(String[] args) throws IOException {
		String languageTag = "en"; // de
		File dataDirectory = new File("/data/r.usbeck/index_dbpedia_39_en"); // "/Users/ricardousbeck";
		String nodeType = "http://dbpedia.org/resource/";// "http://yago-knowledge.org/resource/"
		String edgeType = "http://dbpedia.org/ontology/";// "http://yago-knowledge.org/resource/"

		String TestFile = "datasets/dbpedia-spotlight-nif.ttl";

		DBpediaSpotlightNIFReader reader = new DBpediaSpotlightNIFReader();
		Corpus corpus = reader.read(new File(TestFile).toURL().toString(), "N3");
		log.info("Corpus size: " + corpus.getNumberOfDocuments());

		DisambiguationAlgorithm algo = new NEDAlgo_HITS(dataDirectory, nodeType, edgeType);
		// DisambiguationAlgorithm algo = new NEDAIDADisambiguator();
		// DisambiguationAlgorithm algo = new NEDSpotlightPoster();

		for (int maxDepth = 2; maxDepth <= 2; ++maxDepth) {
			BufferedWriter bw = new BufferedWriter(new FileWriter("Test_" + TestFile.replace("datasets/", "") + "_" + maxDepth + "_27Dez13.txt", true));
			bw.write("input: " + TestFile + "\n");

			algo.setMaxDepth(maxDepth);
			for (double threshholdTrigram = 1; threshholdTrigram > 0.8; threshholdTrigram -= 0.01) {
				algo.setThreshholdTrigram(threshholdTrigram);

				Evaluator ev = new Evaluator(languageTag, corpus, algo);
				ev.fmeasure();
				ev.writeFmeasureToFile(bw);

				System.gc();
			}
			bw.close();
		}
		algo.close();

	}

}