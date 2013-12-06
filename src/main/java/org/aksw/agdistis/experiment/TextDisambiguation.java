package org.aksw.agdistis.experiment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.aksw.agdistis.algorithm.DisambiguationAlgorithm;
import org.aksw.agdistis.algorithm.NEDAlgo_HITS;
import org.aksw.agdistis.algorithm.lda.NEDAlgo_LDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datatypeshelper.io.xml.CorpusXmlReader;
import datatypeshelper.utils.corpus.Corpus;

public class TextDisambiguation {
    private static Logger log = LoggerFactory.getLogger(TextDisambiguation.class);

    public static void main(String[] args) throws IOException, URISyntaxException {
        String languageTag = "en"; // de
        File dataDirectory = new File("/data/r.usbeck/index_dbpedia_39_en"); // "/Users/ricardousbeck";
        File indexDirectory = new File("/data/m.roeder/daten/dbpedia/3.9/AGDISTIS_Index"); // "/Users/ricardousbeck";
        URL inferencerFile = NEDAlgo_LDA.class.getClassLoader().getResource("wiki_en.inferencer");
        URL pipeFile = NEDAlgo_LDA.class.getClassLoader().getResource("wiki_en.pipe");
        // String nodeType = "http://dbpedia.org/resource/";// "http://yago-knowledge.org/resource/"
        // String edgeType = "http://dbpedia.org/ontology/";// "http://yago-knowledge.org/resource/"

        for (String TestFile : new String[] { "datasets/reuters.xml", "datasets/500newsgoldstandard.xml" }) {
            // "german_corpus_new.xml"
            // "datasets/test.xml", "datasets/AIDACorpus.xml"

            CorpusXmlReader reader = new CorpusXmlReader(new File(TestFile));
            Corpus corpus = reader.getCorpus();
            log.info("Corpus size: " + corpus.getNumberOfDocuments());

            DisambiguationAlgorithm algo = NEDAlgo_LDA.createAlgorithm(indexDirectory,
                    new File(inferencerFile.toURI()), new File(pipeFile.toURI()));
            // DisambiguationAlgorithm algo = new NEDAlgo_HITS(dataDirectory, nodeType, edgeType);
            // DisambiguationAlgorithm algo = new NEDAIDADisambiguator();
            // DisambiguationAlgorithm algo = new NEDSpotlightPoster();

            // for (int maxDepth = 1; maxDepth <= 3; ++maxDepth) {
            BufferedWriter bw = new BufferedWriter(new FileWriter("Test_" + TestFile.replace("datasets/", "")
                    + "_29Nov13.txt", true));
            bw.write("input: " + TestFile + "\n");

            // algo.setMaxDepth(maxDepth);
            // for (double threshholdTrigram = 1; threshholdTrigram > 0.0; threshholdTrigram -= 0.01) {
            // algo.setThreshholdTrigram(threshholdTrigram);

            Evaluator ev = new Evaluator(languageTag, corpus, algo);
            ev.fmeasure();
            ev.writeFmeasureToFile(bw);

            System.gc();
            // }
            bw.close();
            // }
            algo.close();
        }
    }
}