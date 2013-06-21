package de.bluekiwi.labs.experiment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.unister.semweb.topicmodeling.io.xml.CorpusXmlReader;
import com.unister.semweb.topicmodeling.utils.corpus.Corpus;
import com.unister.semweb.topicmodeling.utils.doc.Document;
import com.unister.semweb.topicmodeling.utils.doc.DocumentText;
import com.unister.semweb.topicmodeling.utils.doc.ner.NamedEntitiesInText;
import com.unister.semweb.topicmodeling.utils.doc.ner.NamedEntityInText;

import de.bluekiwi.labs.util.SpotlightPoster;

public class SpotlightCompare {
    private static org.slf4j.Logger log = LoggerFactory.getLogger(SpotlightCompare.class);

    public static void main(String[] args) throws Exception {
        SpotlightPoster spot = new SpotlightPoster();
        for (String INPUT_FILE : new String[] { "500newsgoldstandard.xml", "reuters.xml", "german_corpus.xml" }) {

            CorpusXmlReader reader = new CorpusXmlReader(new File(INPUT_FILE));
            Corpus corpus = reader.getCorpus();
            double t = 0, n = 0;
            for (Document document : corpus) {
                System.gc();
                log.info("Text: " + document.getDocumentId() + "/" + corpus.getNumberOfDocuments());
                spot.doTASK(document);
                // TODO post to spotlight, disambiguate is broken
                NamedEntitiesInText namedEntities = document.getProperty(NamedEntitiesInText.class);
                for (NamedEntityInText namedEntity : namedEntities) {
                    if (namedEntity.getLength() > 2) {
                        String correctVotingURL = namedEntity.getNamedEntityUri();
                        if (correctVotingURL.startsWith("rln:"))
                            correctVotingURL = correctVotingURL.replace("rln:", "http://rdflivenews.aksw.org/resource/");
                        if (correctVotingURL.startsWith("dbpr:"))
                            correctVotingURL = correctVotingURL.replace("dbpr:", "http://dbpedia.org/resource/");
                        String disambiguatedURL = spot.findResult(namedEntity.getStartPos());
                        log.info("\t" + correctVotingURL + " => " + disambiguatedURL);
                        if (correctVotingURL != null) {
                            if (correctVotingURL.equals(disambiguatedURL)) {
                                t++;
                                log.info("\t Disambiguated: " + correctVotingURL + " -> " + disambiguatedURL);
                            } else if (correctVotingURL.equals("http://aksw.org/notInWiki") ||
                                    correctVotingURL.startsWith("http://rdflivenews.aksw.org/resource/") ||
                                    correctVotingURL.startsWith("http://de.dbpedia.org/")) {
                                log.info("\t Closed World Assumption: " + correctVotingURL + " -> " + disambiguatedURL);
                            } else {
                                n++;
                                log.info("\t Not disambiguated: " + correctVotingURL + " -> " + disambiguatedURL);
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
            }
            log.error(INPUT_FILE + ": " + t / (t + n) + " t: " + t + " n: " + n);
        }
    }

    private static String markupText(Document document) {
        List<String> textParts = new ArrayList<String>();
        List<NamedEntityInText> labels = document.getProperty(NamedEntitiesInText.class).getNamedEntities();
        Collections.sort(labels);
        String originalText = document.getProperty(DocumentText.class).getText();
        // start with the last label and add the parts of the new text beginning with its end to the array
        // Note that we are expecting that the labels are sorted descending by there position in the text!
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
            }
            else {
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
}
