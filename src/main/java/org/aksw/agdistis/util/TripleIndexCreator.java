package org.aksw.agdistis.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.turtle.TurtleParser;
import org.slf4j.LoggerFactory;

public class TripleIndexCreator {
    private static org.slf4j.Logger log = LoggerFactory.getLogger(TripleIndexCreator.class);

    public static final String N_TRIPLES = "NTriples";
    public static final String TTL = "ttl";
    public static final String TSV = "tsv";
    public static final Version LUCENE_VERSION = Version.LUCENE_44;

    private Analyzer urlAnalyzer;
    private Analyzer literalAnalyzer;
    private DirectoryReader ireader;
    private IndexWriter iwriter;
    private MMapDirectory directory;

    /**
     * @param knowledgeBase
     *            "http://yago-knowledge.org/resource/" or
     *            "http://dbpedia.org/resource/"
     * 
     * @param languageTag
     *            en or de
     * @param dataDirectory
     *            parent directory of index and dump file directory. E.g.,
     *            /data/r.usbeck ---> /data/r.usbeck/index/.., --->
     *            /data/r.usbeck/dbpedia_[LANGUAGE]
     */
    public static void main(String args[]) {
//        if (args.length != 3) {
//            System.err.println("Usage: TripleIndexCreator <index-directory> <data-directory> <language-tag>");
//            return;
//        }
        // String knowledgeBase = "http://yago-knowledge.org/resource/";
//        String knowledgeBase = "http://dbpedia.org/resource/";
        String knowledgeBase = "http://dbpedia.org/resource/";
         String languageTag = "en";
         String indexDirectory = "/data/r.usbeck/indexdbpedia_en";
         String dataDirectory = "/data/r.usbeck/dbpedia_39_data/en";
//        String indexDirectory = args[0];
//        String dataDirectory = args[1];
//        String languageTag = args[2];
        File labelsWithoutRedirectsFile;
        List<File> tmp = new ArrayList<File>();
        if (("http://dbpedia.org/resource/".equals(knowledgeBase)) || ("http://dbpedia.org/resource/".equals(knowledgeBase))) {
            labelsWithoutRedirectsFile = new File(dataDirectory + "/labels_without_redirects_" + languageTag + ".ttl");
            if (labelsWithoutRedirectsFile.exists()) {
                tmp.add(labelsWithoutRedirectsFile);
            } else {
                tmp.add(new File(dataDirectory + "/labels_" + languageTag + ".ttl"));
                tmp.add(new File(dataDirectory + "/redirects_transitive_" + languageTag + ".ttl"));
            }

            tmp.add(new File(dataDirectory + "/instance_types_" + languageTag + ".ttl"));
            tmp.add(new File(dataDirectory + "/mappingbased_properties_" + languageTag + ".ttl"));
            tmp.add(new File(dataDirectory + "/specific_mappingbased_properties_" + languageTag + ".ttl"));
            tmp.add(new File(dataDirectory + "/disambiguations_" + languageTag + ".ttl"));
            tmp.add(new File(dataDirectory + "/long_abstracts_" + languageTag + ".ttl"));
            tmp.add(new File(dataDirectory + "/" + languageTag + "_surface_forms.tsv"));
        } else {
            tmp.add(new File(dataDirectory + "/yagoTypes.ttl"));
            tmp.add(new File(dataDirectory + "/yagoTransitiveType.ttl"));
            tmp.add(new File(dataDirectory + "/yagoFacts.ttl"));
            tmp.add(new File(dataDirectory + "/yagoLiteralFacts.ttl"));
            tmp.add(new File(dataDirectory + "/yagoLabels.ttl"));
            tmp.add(new File(dataDirectory + "/yagoDBpediaInstances.ttl"));
        }
        TripleIndexCreator ic = new TripleIndexCreator();
        ic.createIndex(tmp, indexDirectory, knowledgeBase);
        ic.close();
    }

    public void createIndex(List<File> files, String idxDirectory, String baseURI) {
        try {
            urlAnalyzer = new SimpleAnalyzer(LUCENE_VERSION);
            literalAnalyzer = new LiteralAnalyzer(LUCENE_VERSION);
            Map<String, Analyzer> mapping = new HashMap<String, Analyzer>();
            mapping.put(TripleIndex.FIELD_NAME_SUBJECT, urlAnalyzer);
            mapping.put(TripleIndex.FIELD_NAME_PREDICATE, urlAnalyzer);
            mapping.put(TripleIndex.FIELD_NAME_OBJECT_URI, urlAnalyzer);
            mapping.put(TripleIndex.FIELD_NAME_OBJECT_LITERAL, literalAnalyzer);
            PerFieldAnalyzerWrapper perFieldAnalyzer = new PerFieldAnalyzerWrapper(urlAnalyzer, mapping);

            File indexDirectory = new File(idxDirectory);
            indexDirectory.mkdir();
            directory = new MMapDirectory(indexDirectory);
            IndexWriterConfig config = new IndexWriterConfig(LUCENE_VERSION, perFieldAnalyzer);
            iwriter = new IndexWriter(directory, config);
            iwriter.commit();
            for (File file : files) {
                String type = FilenameUtils.getExtension(file.getName());
                if (type.equals(TTL))
                    indexTTLFile(file, baseURI);
                if (type.equals(TSV))
                    indexTSVFile(file);
                iwriter.commit();
            }
            iwriter.close();
            ireader = DirectoryReader.open(directory);
        } catch (Exception e) {
            log.error("Error while creating TripleIndex.", e);
        }
    }

    private void indexTTLFile(File file, String baseURI) throws RDFParseException, RDFHandlerException,
            FileNotFoundException, IOException {
        log.info("Start parsing: " + file);
        RDFParser parser = new TurtleParser();
        OnlineStatementHandler osh = new OnlineStatementHandler();
        parser.setRDFHandler(osh);
        parser.setStopAtFirstError(false);
        parser.parse(new FileReader(file), baseURI);
        log.info("Finished parsing: " + file);
    }

    private void indexTSVFile(File file) throws IOException {
        log.info("Start parsing: " + file);
        BufferedReader br = new BufferedReader(new FileReader(file));
        while (br.ready()) {
            String[] line = br.readLine().split("\t");
            String subject = line[0];
            for (int i = 1; i < line.length; ++i) {
                String object = line[i];
                Document doc = new Document();
                doc.add(new StringField(TripleIndex.FIELD_NAME_SUBJECT, subject, Store.YES));
                doc.add(new StringField(TripleIndex.FIELD_NAME_PREDICATE,
                        "http://www.w3.org/2004/02/skos/core#altLabel", Store.YES));
                doc.add(new TextField(TripleIndex.FIELD_NAME_OBJECT_LITERAL, object, Store.YES));
                iwriter.addDocument(doc);
            }
        }
        br.close();
        log.info("Finished parsing: " + file);
    }

    private void addDocumentToIndex(IndexWriter iwriter, String subject, String predicate, String object, boolean isUri)
            throws IOException {
        Document doc = new Document();
        log.debug(subject + " " + predicate + " " + object);
        doc.add(new StringField(TripleIndex.FIELD_NAME_SUBJECT, subject, Store.YES));
        doc.add(new StringField(TripleIndex.FIELD_NAME_PREDICATE, predicate, Store.YES));
        if (isUri) {
            doc.add(new StringField(TripleIndex.FIELD_NAME_OBJECT_URI, object, Store.YES));
        } else {
            doc.add(new TextField(TripleIndex.FIELD_NAME_OBJECT_LITERAL, object, Store.YES));
        }
        iwriter.addDocument(doc);
    }

    public void close() {
        try {
            if (ireader != null) {
                ireader.close();
            }
            if (directory != null) {
                directory.close();
            }
        } catch (IOException e) {
            log.error("Error while closing TripleIndex", e);
        }
    }

    private class OnlineStatementHandler extends RDFHandlerBase {
        @Override
        public void handleStatement(Statement st) {
            String subject = st.getSubject().stringValue();
            String predicate = st.getPredicate().stringValue();
            String object = st.getObject().stringValue();
            try {
                addDocumentToIndex(iwriter, subject, predicate, object, st.getObject() instanceof URI);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}