package org.aksw.agdistis.experiment;

import java.io.File;
import java.io.IOException;

import datatypeshelper.io.xml.CorpusXmlReader;
import datatypeshelper.utils.corpus.Corpus;
import datatypeshelper.utils.doc.Document;
import datatypeshelper.utils.doc.DocumentText;
import datatypeshelper.utils.doc.ner.NamedEntitiesInText;

public class Textlength {

    public static void main(String[] args) throws IOException {

        for (String corp : new String[] { "AIDACorpus.xml", "reuters.xml", "german_corpus.xml", "500newsgoldstandard.xml" }) {
            CorpusXmlReader reader = new CorpusXmlReader(new File(corp));
            Corpus corpus = reader.getCorpus();
            double sum = 0;
            double sum_ent = 0;
            double n = 0;
            for (Document document : corpus) {
                double length = document.getProperty(DocumentText.class).getText().length();
                double e = document.getProperty(NamedEntitiesInText.class).getNamedEntities().size();
                sum += length;
                sum_ent += e;
                n++;
            }
            System.out.println(corp + " avg. text length: " + sum / n + " avg. entity number: " + sum_ent / n);
        }
    }
}