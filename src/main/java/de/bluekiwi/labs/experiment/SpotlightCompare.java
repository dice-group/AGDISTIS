package de.bluekiwi.labs.experiment;

import java.io.File;

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
        for (String INPUT_FILE : new String[] { "german_corpus.xml" }) {// "reuters.xml", "500newsgoldstandard.xml"
            CorpusXmlReader reader = new CorpusXmlReader(new File(INPUT_FILE));
            Corpus corpus = reader.getCorpus();
            double t = 0, n = 0;
            for (Document document : corpus) {
                System.gc();
                Thread.sleep(250);
                log.info("Text: " + document.getDocumentId() + "/" + corpus.getNumberOfDocuments());
                spot.doTASK(document);
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
                                    // correctVotingURL.startsWith("http://de.dbpedia.org/") ||
                                    correctVotingURL.startsWith("http://rdflivenews.aksw.org/resource/")) {
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
}