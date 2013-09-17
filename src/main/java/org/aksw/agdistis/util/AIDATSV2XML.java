package org.aksw.agdistis.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/*
 * The format of the final file is the following:
 *
 * - Each document starts with a line: -DOCSTART- (<docid>)
 * - Each following line represents a single token, sentences are separated by an empty line
 *
 * Lines with tabs are tokens the are part of a mention:
 * - column 1 is the token
 * - column 2 is either B (beginning of a mention) or I (continuation of a mention)
 * - column 3 is the full mention used to find entity candidates
 * - column 4 is the corresponding YAGO2 entity (in YAGO encoding, i.e. unicode characters are backslash encoded and spaces are replaced by underscores, see also the tools on the YAGO2 website), OR --NME--, denoting that there is no matching entity in YAGO2 for this particular mention, or that we are missing the connection between the mention string and the YAGO2 entity.
 * - column 5 is the corresponding Wikipedia URL of the entity (added for convenience when evaluating against a Wikipedia based method)
 * - column 6 is the corresponding Wikipedia ID of the entity (added for convenience when evaluating against a Wikipedia based method - the ID refers to the dump used for annotation, 2010-08-17)
 * - column 7 is the corresponding Freebase mid, if there is one (thanks to Massimiliano Ciaramita from Google Zürich for creating the mapping and making it available to us)
 */
public class AIDATSV2XML {

    public static void main(String args[]) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader("AIDA-YAGO2-dataset.tsv"));
        boolean inAMention = false;
        boolean firstDocument = true;
        boolean beginOfADocument = true;

        BufferedWriter bw = new BufferedWriter(new FileWriter("AIDACorpus.xml"));
        // start corpus
        bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        bw.newLine();
        bw.write("<corpus xmlns=\"http://semweb.unister.de/xml-corpus-schema-2013\">");
        bw.newLine();
        int docId = 0;
        while (br.ready()) {
            String line = br.readLine();
            // new document
            if (line.startsWith("-DOCSTART-")) {
                // finish last document xml
                if (!firstDocument) {
                    if (inAMention) {
                        bw.write("</NamedEntityInText>");
                        bw.newLine();
                    } else {
                        bw.write("</SimpleTextPart>");
                        bw.newLine();
                    }

                    bw.write("\t</TextWithNamedEntities>");
                    bw.newLine();
                    bw.write("</Document>");
                    bw.newLine();
                }
                firstDocument = false;
                bw.write("<Document id=\"" + docId + "\">");
                docId++;
                bw.newLine();
                bw.write("\t<TextWithNamedEntities>");
                bw.newLine();
                beginOfADocument = true;
                inAMention = false;
                // } else if (line.equals("")) {
                // bw.write(" ");
            } else {
                String[] splitLine = line.split("\t");
                if (splitLine.length == 1) {
                    // end xml of mention
                    if (inAMention) {
                        bw.write("</NamedEntityInText>");
                        bw.newLine();
                        bw.write("\t\t<SimpleTextPart> ");
                        beginOfADocument = false;
                    }
                    if (beginOfADocument) {
                        bw.write("\t\t<SimpleTextPart> ");
                    }
                    beginOfADocument = false;
                    inAMention = false;
                    bw.write(splitLine[0].replace("&", "&amp;"));
                    bw.write(" ");
                } else if (splitLine[1].equals("B")) {
                    if (!beginOfADocument & !inAMention) {
                        // begin of a mention
                        bw.write("</SimpleTextPart>");
                        bw.newLine();
                    } else if (!beginOfADocument & inAMention) {
                        bw.write("</NamedEntityInText>");
                        bw.newLine();
                    }
                    // correct for dbpedia url
                    String wikipediaURL = null;
                    if (splitLine.length > 4)
                        wikipediaURL = splitLine[4].replace("http://en.wikipedia.org/wiki/", "http://dbpedia.org/resource/");
                    else
                        wikipediaURL = "http://aksw.org/notInWiki";

                    bw.write("\t\t<NamedEntityInText uri=\"" + wikipediaURL.replace("&", "%26") + "\">");
                    bw.write(splitLine[0].replace("&", "&amp;"));
                    inAMention = true;
                    beginOfADocument = false;
                } else if (splitLine[1].equals("I")) {
                    // continuation
                    bw.write(" ");
                    bw.write(splitLine[0].replace("&", "&amp;"));
                    inAMention = true;
                }
            }
        }
        if (inAMention) {
            bw.write("</NamedEntityInText>");
            bw.newLine();
        } else {
            bw.write("</SimpleTextPart>");
            bw.newLine();
        }

        bw.write("\t</TextWithNamedEntities>");
        bw.newLine();
        bw.write("</Document>");
        bw.newLine();
        // finish corpus
        bw.write("</corpus>");
        bw.close();
        br.close();
    }
}
