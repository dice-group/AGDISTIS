package org.aksw.agdistis.algorithm.lda;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.aksw.agdistis.algorithm.DisambiguationAlgorithm;
import org.aksw.agdistis.util.DBPedia;
import org.aksw.agdistis.util.Triple;
import org.aksw.agdistis.util.TripleIndex;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.mallet.pipe.Pipe;
import cc.mallet.topics.LongBasedTopicInferencer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;

import com.unister.semweb.commons.collections.associative.TopDoubleObjectCollection;
import com.unister.semweb.topicmodeling.io.StorageHelper;
import com.unister.semweb.topicmodeling.lang.Language;
import com.unister.semweb.topicmodeling.lang.postagging.PosTaggerFactory;
import com.unister.semweb.topicmodeling.lang.postagging.PosTaggingTermFilter;
import com.unister.semweb.topicmodeling.preprocessing.SingleDocumentPreprocessor;
import com.unister.semweb.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import com.unister.semweb.topicmodeling.preprocessing.docsupplier.decorator.PosTaggingSupplierDecorator;
import com.unister.semweb.topicmodeling.preprocessing.docsupplier.decorator.StemmedTextCreatorSupplierDecorator;

import datatypeshelper.utils.doc.Document;
import datatypeshelper.utils.doc.DocumentText;
import datatypeshelper.utils.doc.ner.NamedEntitiesInText;
import datatypeshelper.utils.doc.ner.NamedEntityInText;

public class NEDAlgo_LDA implements DisambiguationAlgorithm {

    private static final Logger LOGGER = LoggerFactory.getLogger(NEDAlgo_LDA.class);

    private static final int INFERENCER_ITERATIONS = 500;
    private static final int INFERENCER_BURN_IN = 200;
    // thinning is the number of iterations until the next sample is used for the result
    private static final int INFERENCER_THINNING = 50;

    private static final boolean REQUEST_MISSING_ABSTRACTS_FROM_LIVE_ENDPOINT = false;
    private static final String DBPEDIA_LIVE_ENDPOINT = "http://live.dbpedia.org/sparql";

    private static final String ABSTRACT_PREDICAT = "http://dbpedia.org/ontology/abstract";

    private HashMap<Integer, String> algorithmicResult = new HashMap<Integer, String>();
    private TripleIndex index = null;
    // private SubjectPredicateObjectIndex abstractIndex;
    // private Vocabulary vocabulary;
    private SingleDocumentPreprocessor preprocessor;
    private LongBasedTopicInferencer inferencer;
    private Pipe pipe;
    private final Pattern numberPattern = Pattern.compile(".*\\d.*");

    public static NEDAlgo_LDA createAlgorithm(File indexDirectory, File inferencerFile, File pipeFile) {
        TripleIndex index = new TripleIndex(indexDirectory);

        // File abstractIndexDir = new File("/data/m.roeder/daten/dbpedia/abstracts_en");
        // SubjectPredicateObjectIndex abstractIndex = null;
        // if (abstractIndexDir.exists()) {
        // abstractIndex = new SubjectPredicateObjectIndex(abstractIndexDir.getAbsolutePath());
        // } else {
        // abstractIndex = new SubjectPredicateObjectIndex(
        // Arrays.asList(new String[] { "/data/m.roeder/daten/dbpedia/long_abstracts_en.ttl" }),
        // abstractIndexDir.getAbsolutePath(), "http://dbpedia.org/",
        // SubjectPredicateObjectIndex.TTL);
        // }

        // LDA model --> inferencer
        LongBasedTopicInferencer inferencer;
        try {
            if (inferencerFile.getName().endsWith(".gz")) {
                inferencer = LongBasedTopicInferencer.readFromModelStateFile(inferencerFile);
            } else {
                inferencer = LongBasedTopicInferencer.read(inferencerFile);
            }
        } catch (Exception e) {
            LOGGER.error("Couldn't load topic model inferencer. Returning null.", e);
            return null;
        }

        Pipe pipe = StorageHelper.readFromFileSavely(pipeFile.getAbsolutePath());
        if (pipe == null) {
            LOGGER.error("Couldn't load the mallet preprocessing pipe from file. Returning null.");
            return null;
        }
        // Load vocabulary
        // final Vocabulary vocabulary;
        // try {
        // vocabulary = StorageHelper.readFromFile(vocabularyFile.getAbsolutePath());
        // } catch (Exception e) {
        // LOGGER.error("Couldn't load vocabulary. Returning null.");
        // return null;
        // }
        final Alphabet alphabet = inferencer.getAlphabet();

        // create Preprocessing
        SingleDocumentPreprocessor preprocessor = new SingleDocumentPreprocessor();
        DocumentSupplier supplier = preprocessor;
        supplier = new PosTaggingSupplierDecorator(supplier, PosTaggerFactory.getPosTaggingStep(Language.ENG,
                new PosTaggingTermFilter() {
                    private Pattern numberPattern = Pattern.compile(".*\\d.*");

                    @Override
                    public boolean isTermGood(com.unister.semweb.ml.text.features.Term term) {
                        // return vocabulary.getId(term.getLemma()) != Vocabulary.WORD_NOT_FOUND;
                        // XXX ADDED CONDITIONS FOR DEBUGGING!
                        String lemma = term.getLemma();
                        return alphabet.contains(lemma) && (lemma.length() > 2)
                                && (!numberPattern.matcher(lemma).matches());
                    }
                }));
        supplier = new StemmedTextCreatorSupplierDecorator(supplier);
        preprocessor.setDocumentSupplier(supplier);

        return new NEDAlgo_LDA(index, /* abstractIndex, vocabulary, */preprocessor, inferencer, pipe);
    }

    public NEDAlgo_LDA(TripleIndex index, /* SubjectPredicateObjectIndex abstractIndex, Vocabulary vocabulary, */
            SingleDocumentPreprocessor preprocessor, LongBasedTopicInferencer inferencer, Pipe pipe) {
        // this.abstractIndex = abstractIndex;
        this.index = index;
        // this.vocabulary = vocabulary;
        this.preprocessor = preprocessor;
        this.inferencer = inferencer;
        this.pipe = pipe;
    }

    private Instance preprocess(String text) {
        com.unister.semweb.topicmodeling.utils.doc.Document tempDoc = new com.unister.semweb.topicmodeling.utils.doc.Document();
        tempDoc.addProperty(new com.unister.semweb.topicmodeling.utils.doc.DocumentText(text));
        preprocessor.getCorpus(tempDoc);

        // create an instance using the pipe of the training data
        text = tempDoc.getProperty(com.unister.semweb.topicmodeling.utils.doc.DocumentText.class).getText();
        Instance instance = new Instance(text, "NED", "hash" + text.hashCode(), null);
        instance = pipe.instanceFrom(instance);
        return instance;
    }

    @Override
    public void run(Document document) {
        algorithmicResult = new HashMap<Integer, String>();
        NamedEntitiesInText namedEntities = document.getProperty(NamedEntitiesInText.class);
        if (namedEntities == null) {
            LOGGER.error("Got a document without the needed NamedEntitiesInText property. Aborting.");
            return;
        }
        DocumentText docText = document.getProperty(DocumentText.class);
        if (docText == null) {
            LOGGER.error("Got a document without the needed DocumentText property. Aborting.");
            return;
        }
        String text = docText.getText();

        Instance docAsInstance = preprocess(text);

        double topicVector[] = inferencer
                .getSampledDistribution(docAsInstance, INFERENCER_ITERATIONS, INFERENCER_THINNING, INFERENCER_BURN_IN);
        double topicVectorLength = getVectorLength(topicVector);

        String surfaceForm;
        Set<String> candidates;
        String bestCandidateURI;
        for (NamedEntityInText entity : namedEntities) {
            // Get all candidates for this entity
            surfaceForm = text.substring(entity.getStartPos(), entity.getEndPos());
            candidates = searchCandidatesByLabel(surfaceForm, true);
            if (candidates.size() > 0) {
                bestCandidateURI = determineBestCandidate(topicVector, topicVectorLength, candidates);
                // add the best candidate
                algorithmicResult.put(entity.getStartPos(), bestCandidateURI);
            }
            // TODO what should be done if there is no candidate?
        }
    }

    private Set<String> searchCandidatesByLabel(String label, boolean searchInSurfaceFormsToo) {
        ArrayList<Triple> tmp = new ArrayList<Triple>();
        tmp.addAll(index.search(null, "http://www.w3.org/2000/01/rdf-schema#label", label));
        if (searchInSurfaceFormsToo) {
            tmp.addAll(index.search(null, "http://www.w3.org/2004/02/skos/core#altLabel", label));
            tmp.addAll(index.search(null, "http://xmlns.com/foaf/0.1/name", label));
        }
        Set<String> candidates = new HashSet<String>();
        for (Triple triple : tmp) {
            candidates.add(triple.getSubject());
        }
        return candidates;
    }

    @SuppressWarnings("unused")
    private String determineBestCandidate(double[] topicVector, double topicVectorLength, Set<String> candidates) {
        TopDoubleObjectCollection<String> topCandidates = new TopDoubleObjectCollection<String>(50, false);
        String entityText, encodedUri = null;
        Instance entityAsInstance;
        // double currentValue, bestValue = -1;
        double entityVector[];
        for (String candidateURI : candidates) {
            // If this URI doesn't contain numbers
            if (!numberPattern.matcher(candidateURI).matches()) {
                // get the abstract of the entity
                entityText = getAbstract(candidateURI);
                if (entityText == null) {
                    int pos = candidateURI.lastIndexOf('/') + 1;
                    try {
                        encodedUri = candidateURI.substring(0, pos)
                                + URLEncoder.encode(candidateURI.substring(pos), "UTF-8");
                        entityText = getAbstract(encodedUri);
                    } catch (UnsupportedEncodingException e) {
                        // This shouldn't happen
                        LOGGER.error("Error while encoding URL.", e);
                    }
                }

                if ((entityText == null) && REQUEST_MISSING_ABSTRACTS_FROM_LIVE_ENDPOINT) {
                    entityText = requestAbstract(encodedUri);
                }

                if (entityText != null) {
                    if (entityText.length() > 0) {
                        entityAsInstance = preprocess(entityText);
                        entityVector = inferencer
                                .getSampledDistribution(entityAsInstance, INFERENCER_ITERATIONS, INFERENCER_THINNING,
                                        INFERENCER_BURN_IN);
                        // currentValue = getSimilarity(topicVector, topicVectorLength, entityVector,
                        // getVectorLength(entityVector));
                        // if (currentValue > bestValue) {
                        // bestUri = candidateURI;
                        // bestValue = currentValue;
                        // }
                        topCandidates.add(getSimilarity(topicVector, topicVectorLength, entityVector,
                                getVectorLength(entityVector)), candidateURI);
                    } else {
                        LOGGER.warn("Got an empty abstract for \"" + candidateURI + "\". Ignoring this candidate.");
                    }
                } else {
                    LOGGER.warn("Couldn't get abstract for \"" + candidateURI + "\". Ignoring this candidate.");
                }
            }
        }
        // System.out.println("****************");
        // for (int i = 0; i < topCandidates.size(); i++) {
        // System.out.println(topCandidates.values[i] + " , " + topCandidates.objects[i]);
        // }
        // System.out.println();
        // return bestUri;
        return (String) topCandidates.objects[0];
    }

    private String requestAbstract(String encodedUri) {
        String entityAbstract = null;
        try {
            DBPedia dbPedia = new DBPedia(DBPEDIA_LIVE_ENDPOINT);
            ArrayList<ArrayList<String>> rows = dbPedia.askDbpedia("SELECT * WHERE { <" + encodedUri
                    + "> <http://dbpedia.org/ontology/abstract> ?o. FILTER langMatches( lang(?o), \"en\" ) }");
            if (rows.size() > 0) {
                entityAbstract = rows.get(0).get(0);
                // abstractIndex.writeToIndex(encodedUri, "http://dbpedia.org/ontology/abstract", entityAbstract);
            }
        } catch (RepositoryException e) {
            LOGGER.error("Error while requesting abstract fpr \"" + encodedUri
                    + "\" from dbpedia live endpoint. Returning null.", e);
        }
        return entityAbstract;
    }

    private String getAbstract(String uri) {
        List<Triple> triples;
        String entityAbstract = null;
        triples = index.search(uri, ABSTRACT_PREDICAT, null);
        // triples = abstractIndex.search(uri);
        if ((triples != null) && (triples.size() > 0)) {
            for (Triple t : triples) {
                if ("http://dbpedia.org/ontology/abstract".equals(t.getPredicate())) {
                    entityAbstract = t.getObject();
                }
            }
        }
        return entityAbstract;
    }

    protected static double getVectorLength(double[] vector) {
        double sum = 0;
        for (int i = 0; i < vector.length; i++) {
            sum += vector[i] * vector[i];
        }
        return Math.sqrt(sum);
    }

    protected static double getSimilarity(double[] topicVector, double topicVectorLength, double[] entityVector,
            double entityVectorLength) {
        double sum = 0;
        for (int i = 0; i < topicVector.length; i++) {
            sum += topicVector[i] * entityVector[i];
        }
        return sum / (topicVectorLength * entityVectorLength);
    }

    @Override
    public String findResult(NamedEntityInText namedEntity) {
        if (algorithmicResult.containsKey(namedEntity.getStartPos())) {
            LOGGER.debug("\t result  " + algorithmicResult.get(namedEntity.getStartPos()));
            return algorithmicResult.get(namedEntity.getStartPos());
        } else {
            LOGGER.debug("\t result null means that we have no candidate for this NE");
            return null;
        }
    }

    @Override
    public void close() {
        index.close();
    }

    @Override
    public String getRedirect(String findResult) {
        return findResult;
    }

    @Override
    public double getThreshholdTrigram() {
        return 0;
    }

    @Override
    public void setThreshholdTrigram(double threshholdTrigram) {
    }

    @Override
    public void setMaxDepth(int maxDepth) {
    }
}