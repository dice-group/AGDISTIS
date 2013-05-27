package de.bluekiwi.labs.ner;

import java.io.IOException;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;

public class StanfordNER {

    public static void main(String[] args) throws IOException {
        // String serializedClassifier = "classifiers/english.all.3class.distsim.crf.ser.gz";
        String serializedClassifier = "classifiers/dewac_175m_600.crf.ser.gz";
        // String text = " Chancelor Angela Merkel will go meet Barack Obama President of the U.S. at Berlin";
        String text = "Bundeskanzlerin Angela Merkel trifft sich mit US-Präsident Obama bei Siemens in Berlin";
        System.out.println(runNER(text, serializedClassifier));
    }

    public static String runNER(String text, String serializedClassifier) throws IOException {
        AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier
                .getClassifierNoExceptions(serializedClassifier);
        return (classifier.classifyWithInlineXML(text));
    }

}
