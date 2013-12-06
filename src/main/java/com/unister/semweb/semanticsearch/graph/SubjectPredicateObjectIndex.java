package com.unister.semweb.semanticsearch.graph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.aksw.agdistis.util.Triple;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.ntriples.NTriplesParser;
import org.openrdf.rio.turtle.TurtleParser;
import org.slf4j.LoggerFactory;

public class SubjectPredicateObjectIndex {
    private org.slf4j.Logger log = LoggerFactory.getLogger(SubjectPredicateObjectIndex.class);
    private String FIELD_NAME_SUBJECT = "subject";
    private String FIELD_NAME_PREDICATE = "predicat";
    private String FIELD_NAME_OBJECT = "object";
    public static final String N_TRIPLES = "NTriples";
    public static final String TTL = "TTL";

    private Directory directory;
    private Analyzer analyzer;
    private IndexSearcher isearcher;
    private QueryParser parser;
    private DirectoryReader ireader;

    // private HashMap<String, List<Triple>> cache;

    public SubjectPredicateObjectIndex(List<String> files, String idxDirectory, String baseURI, String type) {
        init(files, idxDirectory, baseURI, type);
        // cache = new HashMap<String, List<Triple>>();
    }

    public SubjectPredicateObjectIndex(String idxDirectory) {
        try {
            analyzer = new KeywordAnalyzer();
            File indexDirectory = new File(idxDirectory);
            directory = new MMapDirectory(indexDirectory);
            // directory = new RAMDirectory(new SimpleFSDirectory(indexDirectory), IOContext.READ);
            ireader = DirectoryReader.open(directory);
            isearcher = new IndexSearcher(ireader);

            parser = new QueryParser(Version.LUCENE_44, FIELD_NAME_SUBJECT, analyzer);
            parser.setDefaultOperator(QueryParser.Operator.AND);
            // cache = new HashMap<String, List<Triple>>();
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
        }
    }

    public void init(List<String> files, String idxDirectory, String baseURI, String type) {
        try {
            analyzer = new KeywordAnalyzer();
            File indexDirectory = new File(idxDirectory);
            if (indexDirectory.exists() && indexDirectory.isDirectory() && indexDirectory.listFiles().length > 0) {
                directory = new MMapDirectory(indexDirectory);
            } else {
                indexDirectory.mkdir();
                directory = new MMapDirectory(indexDirectory);
                IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_44, analyzer);
                IndexWriter iwriter = new IndexWriter(directory, config);
                for (String file : files)
                {
                    if (type.equals(TTL))
                        indexTTLFile(file, baseURI, iwriter);
                    if (type.equals(N_TRIPLES))
                        indexNTriplesFile(file, baseURI, iwriter);

                }
                iwriter.close();
            }
            ireader = DirectoryReader.open(directory);
            isearcher = new IndexSearcher(ireader);

            parser = new QueryParser(Version.LUCENE_44, FIELD_NAME_SUBJECT, analyzer);
            parser.setDefaultOperator(QueryParser.Operator.AND);
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
        }
    }

    private void indexTTLFile(String file, String baseURI, IndexWriter iwriter) {
        try {
            log.info("Start parsing: " + file);
            RDFParser parser = new TurtleParser();
            OnlineStatementHandler osh = new OnlineStatementHandler(iwriter);
            parser.setRDFHandler(osh);
            parser.setStopAtFirstError(false);
            parser.parse(new FileReader(file), baseURI);
            log.info("Finished parsing: " + file);
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
        } catch (RDFParseException e) {
            log.error(e.getLocalizedMessage());
        } catch (RDFHandlerException e) {
            log.error(e.getLocalizedMessage());
        }

    }

    private void indexNTriplesFile(String file, String baseUri, IndexWriter iwriter) {
        try {
            log.info("Start parsing: " + file);
            RDFParser parser = new NTriplesParser();
            OnlineStatementHandler osh = new OnlineStatementHandler(iwriter);
            parser.setRDFHandler(osh);
            parser.setStopAtFirstError(false);
            parser.parse(new FileReader(file), baseUri);
            log.info("Finished parsing: " + file);
        } catch (RDFParseException e) {
            log.error(e.getLocalizedMessage());
        } catch (RDFHandlerException e) {
            log.error(e.getLocalizedMessage());
        } catch (FileNotFoundException e) {
            log.error(e.getLocalizedMessage());
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
        }
    }

    private void addDocumentToIndex(IndexWriter iwriter, String subject, String predicate, String object) {
        Document doc = new Document();
        doc.add(new StringField(FIELD_NAME_SUBJECT, subject, Store.YES));
        doc.add(new StringField(FIELD_NAME_PREDICATE, predicate, Store.YES));
        doc.add(new StringField(FIELD_NAME_OBJECT, object, Store.YES));
        try {
            iwriter.addDocument(doc);
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
        }
    }

    public List<Triple> search(String subject) {
        // if (cache.containsKey(subject)) {
        // return cache.get(subject);
        // }
        List<Triple> triples = new ArrayList<Triple>();
        try {
            log.debug("\t start asking index...");
            TermQuery tq = new TermQuery(new Term(FIELD_NAME_SUBJECT, subject));
            BooleanQuery bq = new BooleanQuery();
            bq.add(tq, BooleanClause.Occur.SHOULD);
            TopScoreDocCollector collector = TopScoreDocCollector.create(1000, true);
            isearcher.search(bq, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;

            for (int i = 0; i < hits.length; i++) {
                Document hitDoc = isearcher.doc(hits[i].doc);
                String object = java.net.URLDecoder.decode(hitDoc.get(FIELD_NAME_OBJECT), "UTF-8");
                String predicate = hitDoc.get(FIELD_NAME_PREDICATE);
                triples.add(new Triple(subject, predicate, object));
            }
            log.debug("\t finished asking index...");
        } catch (Exception e) {
            log.warn(e.getLocalizedMessage() + " -> " + subject);
        }
        // cache.put(subject, triples);
        return triples;
    }

    public List<Triple> searchAsObject(String object) {
        // if (cache.containsKey(subject)) {
        // return cache.get(subject);
        // }
        List<Triple> triples = new ArrayList<Triple>();
        try {
            log.debug("\t start asking index...");
            TermQuery tq = new TermQuery(new Term(FIELD_NAME_OBJECT, object));
            BooleanQuery bq = new BooleanQuery();
            bq.add(tq, BooleanClause.Occur.SHOULD);
            TopScoreDocCollector collector = TopScoreDocCollector.create(1000, true);
            isearcher.search(bq, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;

            for (int i = 0; i < hits.length; i++) {
                Document hitDoc = isearcher.doc(hits[i].doc);
                String subject = java.net.URLDecoder.decode(hitDoc.get(FIELD_NAME_SUBJECT), "UTF-8");
                String predicate = hitDoc.get(FIELD_NAME_PREDICATE);
                triples.add(new Triple(subject, predicate, object));
            }
            log.debug("\t finished asking index...");
        } catch (Exception e) {
            log.warn(e.getLocalizedMessage() + " -> o:" + object);
        }
        // cache.put(subject, triples);
        return triples;
    }

    public int getOutDegree(String subject) {
        try {
            log.debug("\t start asking index...");
            TermQuery tq = new TermQuery(new Term(FIELD_NAME_SUBJECT, subject));
            BooleanQuery bq = new BooleanQuery();
            bq.add(tq, BooleanClause.Occur.SHOULD);
            TotalHitCountCollector collector = new TotalHitCountCollector();
            isearcher.search(bq, collector);
            return collector.getTotalHits();
        } catch (Exception e) {
            log.warn(e.getLocalizedMessage() + " -> s:" + subject);
        }
        return -1;
    }

    public int getInDegree(String object) {
        try {
            log.debug("\t start asking index...");
            TermQuery tq = new TermQuery(new Term(FIELD_NAME_OBJECT, object));
            BooleanQuery bq = new BooleanQuery();
            bq.add(tq, BooleanClause.Occur.SHOULD);
            TotalHitCountCollector collector = new TotalHitCountCollector();
            isearcher.search(bq, collector);
            return collector.getTotalHits();
        } catch (Exception e) {
            log.warn(e.getLocalizedMessage() + " -> o:" + object);
        }
        return -1;
    }

    public void close() {
        try {
            ireader.close();
            directory.close();
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
        }
    }

    public void writeToIndex(String subject, String predicate, String object) {
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_44, analyzer);
        IndexWriter iwriter = null;
        try {
            iwriter = new IndexWriter(directory, config);
            addDocumentToIndex(iwriter, subject, predicate, object);
            iwriter.commit();
        } catch (IOException e) {
            log.error("Couldn't add new document to index.", e);
            e.printStackTrace();
        } finally {
            if (iwriter != null) {
                try {
                    iwriter.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private class OnlineStatementHandler extends RDFHandlerBase {
        private IndexWriter iwriter;

        public OnlineStatementHandler(IndexWriter iwriter) {
            this.iwriter = iwriter;
        }

        @Override
        public void handleStatement(Statement st) {
            String subject = st.getSubject().stringValue();
            String object = st.getObject().stringValue();
            String predicate = st.getPredicate().stringValue();
            addDocumentToIndex(iwriter, subject, predicate, object);
        }
    }
}
