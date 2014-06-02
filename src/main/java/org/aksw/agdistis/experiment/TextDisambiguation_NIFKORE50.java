package org.aksw.agdistis.experiment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;

import org.aksw.agdistis.algorithm.DisambiguationAlgorithm;
//import org.aksw.agdistis.algorithm.NEDAIDADisambiguator;
import org.aksw.agdistis.algorithm.NEDAlgo_HITS;
import org.aksw.agdistis.algorithm.NEDSpotlightPoster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datatypeshelper.io.Kore50NIFReader;
import datatypeshelper.utils.corpus.Corpus;

public class TextDisambiguation_NIFKORE50 {
	private static Logger log = LoggerFactory.getLogger(TextDisambiguation_NIFKORE50.class);

	public static void main(String[] args) throws IOException {
		String languageTag = "en"; // de
		File dataDirectory = new File("/data/r.usbeck/index_dbpedia_39_en"); // "/Users/ricardousbeck";
		String nodeType = "http://dbpedia.org/resource/";// "http://yago-knowledge.org/resource/"
		String edgeType = "http://dbpedia.org/ontology/";// "http://yago-knowledge.org/resource/"

		String TestFile = "datasets/kore50-nif.ttl";

		Kore50NIFReader reader = new Kore50NIFReader();
		Corpus corpus = reader.read(new File(TestFile).toURL().toString(), "N3");
		log.info("Corpus size: " + corpus.getNumberOfDocuments());
		HashSet<DisambiguationAlgorithm> algs = new HashSet<DisambiguationAlgorithm>();
		algs.add(new NEDAlgo_HITS(dataDirectory, nodeType, edgeType));
//		algs.add(new NEDAIDADisambiguator());
		algs.add(new NEDSpotlightPoster());
		for (DisambiguationAlgorithm algo : algs) {
			if (algo instanceof NEDAlgo_HITS) {
				for (int maxDepth = 1; maxDepth <= 3; ++maxDepth) {
					BufferedWriter bw = new BufferedWriter(new FileWriter("Test_" + TestFile.replace("datasets/", "") + "_" + maxDepth + "_" + algo.toString() + "_27Dez13.txt", true));
					bw.write("input: " + TestFile + "\n");

					algo.setMaxDepth(maxDepth);
					for (double threshholdTrigram = 1; threshholdTrigram > 0.5; threshholdTrigram -= 0.01) {
						algo.setThreshholdTrigram(threshholdTrigram);

						Evaluator ev = new Evaluator(languageTag, corpus, algo);
						ev.fmeasure();
						ev.writeFmeasureToFile(bw);

						System.gc();
					}
					bw.close();
				}
				algo.close();
			} else {
				BufferedWriter bw = new BufferedWriter(new FileWriter("Test_" + TestFile.replace("datasets/", "") + "_" + algo.toString() + "_27Dez13.txt", true));
				bw.write("input: " + TestFile + "\n");

				Evaluator ev = new Evaluator(languageTag, corpus, algo);
				ev.fmeasure();
				ev.writeFmeasureToFile(bw);

				System.gc();
				bw.close();
			}
			algo.close();
		}
	}
}