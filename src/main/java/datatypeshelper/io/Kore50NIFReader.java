package datatypeshelper.io;

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

public class Kore50NIFReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(Kore50NIFReader.class);

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
        Map<String, Sentence> uriSentencMapping = new HashMap<String, Sentence>();
        Resource individual;
        String individualUri, documentUri, tokenUri, namedEntityUri, text;
        int indizes[];
        Sentence sentence;
        while (iterator.hasNext()) {
            individual = iterator.next();
            individualUri = individual.getURI();
            // If this is a sentence
            if (individual.hasProperty(nifBroaderContext)) {
                documentUri = individualUri;
                text = individual.getProperty(nifIsString).getString();
                if (documentUri != null) {
                    // if this document doesn't already exist
                    if (!uriSentencMapping.containsKey(documentUri)) {
                        uriSentencMapping.put(documentUri, new Sentence(text));
                    } else {
                        uriSentencMapping.get(documentUri).text = text;
                    }
                } else {
                    LOGGER.error("Problem: documentUri = \"" + documentUri + "\"");
                }
            }
            // If this is a named entity
            if (individual.hasProperty(nifReferenceContext) && individual.hasProperty(itsrdfIdentRef)) {
                tokenUri = individualUri;
                namedEntityUri = individual.getProperty(itsrdfIdentRef).getObject().toString();
                documentUri = individual.getProperty(nifReferenceContext).getObject().toString();
                indizes = getIndizes(tokenUri);
                if ((documentUri != null) && (indizes != null)) {
                    // if this document doesn't already exist
                    if (!uriSentencMapping.containsKey(documentUri)) {
                        sentence = new Sentence(null);
                        uriSentencMapping.put(documentUri, sentence);
                    } else {
                        sentence = uriSentencMapping.get(documentUri);
                    }
                    sentence.namedEntities.add(new NamedEntityInText(indizes[0], indizes[1] - indizes[0],
                            namedEntityUri));
                } else {
                    LOGGER.error("Couldn't get documentUri and indizes from \"" + tokenUri + "\".");
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

    private List<Document> createDocumentList(Map<String, Sentence> uriSentencMapping) {
        Sentence sentence;
        List<Document> documents = new ArrayList<Document>();
        Document document;
        int docId = 0;
        NamedEntitiesInText neInText;
        for (String documentUri : uriSentencMapping.keySet()) {
            sentence = uriSentencMapping.get(documentUri);
            document = new Document(docId);
            ++docId;
            document.addProperty(new DocumentURI(documentUri));
            neInText = new NamedEntitiesInText();
            for (NamedEntityInText namedEntity : sentence.namedEntities) {
                neInText.addNamedEntity(namedEntity);
            }
            document.addProperty(neInText);
            document.addProperty(new DocumentText(sentence.text));
            documents.add(document);
        }
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
