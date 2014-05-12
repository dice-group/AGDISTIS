package org.aksw.agdistis.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFReader;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import datatypeshelper.utils.corpus.Corpus;
import datatypeshelper.utils.corpus.DocumentListCorpus;
import datatypeshelper.utils.doc.Document;
import datatypeshelper.utils.doc.DocumentText;
import datatypeshelper.utils.doc.DocumentURI;
import datatypeshelper.utils.doc.ner.NamedEntitiesInText;
import datatypeshelper.utils.doc.ner.NamedEntityInText;

public class DBpediaSpotlightNIFReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBpediaSpotlightNIFReader.class);

    public Corpus read(String nifFile, String rdfLang) {
        // ModelMaker mMaker = ModelFactory.createFileModelMaker(nifFile);
        // ExtendedIterator<String> mIter = mMaker.listModels();
        // Model model;
        // if(mIter.hasNext()) {
        // String modelUri = mIter.next();
        // model = mMaker.getModel(modelUri);
        // } else {
        // return null;
        // }
        OntModel model;
        model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        RDFReader reader = model.getReader(rdfLang);
        reader.read(model, nifFile);

        Property nifBroaderContext = model
                .getProperty("http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#broaderContext");
        Property nifReferenceContext = model
                .getProperty("http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#referenceContext");
        Property nifIsString = model
                .getProperty("http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#isString");
        // Property nifEndIndex = model
        // .getProperty("http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#endIndex");
        Property itsrdfIdentRef = model.getProperty("http://www.w3.org/2005/11/its/rdf#taIdentRef");

        ExtendedIterator<Resource> iterator = model.listSubjects();
        Map<String, List<Sentence>> uriSentencMapping = new HashMap<String, List<Sentence>>();
        Resource individual;
        String individualUri, documentUri, sentenceUri, tokenUri, namedEntityUri, text;
        int sentenceId;
        int indizes[];
        Sentence sentence;
        List<Sentence> sentences;
        while (iterator.hasNext()) {
            individual = iterator.next();
            individualUri = individual.getURI();
            // If this is a sentence
            if (individual.hasProperty(nifBroaderContext)) {
                sentenceUri = individualUri;
                documentUri = getDocUriFromSentence(sentenceUri);
                sentenceId = getSentenceId(sentenceUri);
                text = individual.getProperty(nifIsString).getString();
                if ((documentUri != null) && (sentenceId >= 0)) {
                    // if this document doesn't already exist
                    if (!uriSentencMapping.containsKey(documentUri)) {
                        sentences = new ArrayList<Sentence>();
                        uriSentencMapping.put(documentUri, sentences);
                    } else {
                        sentences = uriSentencMapping.get(documentUri);
                    }
                    while (sentences.size() <= sentenceId) {
                        sentences.add(null);
                    }
                    // if the sentence object already exists (because a NE of this sentence created it)
                    if (sentences.get(sentenceId) != null) {
                        sentences.get(sentenceId).text = text;
                    } else {
                        sentences.set(sentenceId, new Sentence(text));
                    }
                } else {
                    LOGGER.error("Problem: documentUri = \"" + documentUri + "\", sentenceId = " + sentenceId);
                }
            }
            // If this is a named entity
            if (individual.hasProperty(nifReferenceContext) && individual.hasProperty(itsrdfIdentRef)) {
                tokenUri = individualUri;
                namedEntityUri = individual.getProperty(itsrdfIdentRef).getObject().toString();
                sentenceUri = getSentenceUriFromToken(tokenUri);
                indizes = getIndizes(tokenUri);
                if ((sentenceUri != null) && (indizes != null)) {
                    documentUri = getDocUriFromSentence(sentenceUri);
                    sentenceId = getSentenceId(sentenceUri);
                    if ((documentUri != null) && (sentenceId >= 0)) {
                        // if this document doesn't already exist
                        if (!uriSentencMapping.containsKey(documentUri)) {
                            sentences = new ArrayList<Sentence>();
                            uriSentencMapping.put(documentUri, sentences);
                        } else {
                            sentences = uriSentencMapping.get(documentUri);
                        }
                        while (sentences.size() <= sentenceId) {
                            sentences.add(null);
                        }
                        // if the sentence object already exists (because a NE of this sentence created it)
                        if (sentences.get(sentenceId) == null) {
                            sentence = new Sentence(null);
                            sentences.set(sentenceId, sentence);
                        } else {
                            sentence = sentences.get(sentenceId);
                        }
                        sentence.namedEntities.add(new NamedEntityInText(indizes[0], indizes[1] - indizes[0],
                                namedEntityUri));
                    } else {
                        LOGGER.error("Problem: documentUri = \"" + documentUri + "\", sentenceId = " + sentenceId);
                    }
                } else {
                    LOGGER.error("Couldn't get SentenceUri and indizes from \"" + tokenUri + "\".");
                }
            }
        }

        return new DocumentListCorpus<List<Document>>(createDocumentList(uriSentencMapping));
    }

    private int[] getIndizes(String tokenUri) {
        int pos, beginIndex, endIndex;
        pos = tokenUri.lastIndexOf("=");
        endIndex = tokenUri.lastIndexOf(",");
        if ((pos >= 0) && (endIndex > 0)) {
            beginIndex = Integer.parseInt(tokenUri.substring(pos + 1, endIndex));
            pos = endIndex;
            endIndex = Integer.parseInt(tokenUri.substring(pos + 1));
            return new int[] { beginIndex, endIndex };
        } else {
            return null;
        }
    }

    private String getSentenceUriFromToken(String tokenUri) {
        int pos = tokenUri.indexOf(",char=");
        if (pos >= 0) {
            return tokenUri.substring(0, pos);
        } else {
            return null;
        }
    }

    private String getDocUriFromSentence(String sentenceUri) {
        int pos = sentenceUri.indexOf("#sentence");
        if (pos >= 0) {
            return sentenceUri.substring(0, pos);
        } else {
            return null;
        }
    }

    private int getSentenceId(String sentenceUri) {
        int pos = sentenceUri.lastIndexOf("=");
        if (pos >= 0) {
            return Integer.parseInt(sentenceUri.substring(pos + 1));
        } else {
            return -1;
        }
    }

    private List<Document> createDocumentList(Map<String, List<Sentence>> uriSentencMapping) {
        int sentenceCount = 0;
        List<Sentence> sentences;
        List<Document> documents = new ArrayList<Document>();
        Document document;
        int docId = 0;
        NamedEntitiesInText neInText;
        StringBuilder textBuilder = new StringBuilder();
        for (String documentUri : uriSentencMapping.keySet()) {
            sentences = uriSentencMapping.get(documentUri);
            document = new Document(docId);
            ++docId;
            document.addProperty(new DocumentURI(documentUri));
            neInText = new NamedEntitiesInText();
            textBuilder.delete(0, textBuilder.length());
            for (Sentence sentence : sentences) {
                textBuilder.append(sentence.text);
                textBuilder.append(' ');
                for (NamedEntityInText namedEntity : sentence.namedEntities) {
                    neInText.addNamedEntity(namedEntity);
                }
                ++sentenceCount;
            }
            document.addProperty(neInText);
            document.addProperty(new DocumentText(textBuilder.toString()));
            documents.add(document);
        }
        LOGGER.info("Saw " + sentenceCount + " sentences.");
        return documents;
    }

    private static class Sentence {
        public List<NamedEntityInText> namedEntities = new ArrayList<NamedEntityInText>();
        public String text;

        public Sentence(String text) {
            this.text = text;
        }
    }
}
