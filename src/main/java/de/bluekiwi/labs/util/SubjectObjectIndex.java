package de.bluekiwi.labs.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.ntriples.NTriplesParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unister.semweb.ned.QRToolNED.datatypes.Candidate;

public class SubjectObjectIndex {
    private Logger log = LoggerFactory.getLogger(SubjectObjectIndex.class);
    private String FIELD_NAME_SUBJECT = "subject";
    private String FIELD_NAME_OBJECT = "object";
    private Directory directory;
    private Analyzer analyzer;
    private IndexSearcher isearcher;
    private QueryParser parser;
    private DirectoryReader ireader;
    private IndexWriter iwriter;
    private String idxDirectory = "/data/r.usbeck/index/";
    // private String idxDirectory = "/Users/ricardousbeck/index/";
    private String baseURI;

    public SubjectObjectIndex(String file) throws IOException, RDFParseException, RDFHandlerException
    {
        baseURI = "http://dbpedia.org/resource/";
        analyzer = new KeywordAnalyzer();
        idxDirectory = idxDirectory + file;
        File indexDirectory = new File(idxDirectory);
        if (indexDirectory.exists() && indexDirectory.isDirectory() && indexDirectory.listFiles().length > 0)
        {
            directory = new MMapDirectory(indexDirectory);
        } else {
            indexDirectory.mkdir();
            directory = new MMapDirectory(indexDirectory);
            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40, analyzer);
            iwriter = new IndexWriter(directory, config);
            indexNTriplesFile(file);
        }
        ireader = DirectoryReader.open(directory);
        isearcher = new IndexSearcher(ireader);

        parser = new QueryParser(Version.LUCENE_40, FIELD_NAME_SUBJECT, analyzer);
        parser.setDefaultOperator(QueryParser.Operator.AND);
    }

    private void indexNTriplesFile(String file) throws IOException, RDFParseException, RDFHandlerException {
        RDFParser parser = new NTriplesParser();
        OnlineStatementHandler osh = new OnlineStatementHandler();
        parser.setRDFHandler(osh);
        parser.setStopAtFirstError(false);

        parser.parse(new FileReader(file), baseURI);
        iwriter.close();
    }

    private void addDocumentToIndex(IndexWriter iwriter, String subject, String object) throws IOException {
        Document doc = new Document();
        // don't save url locator of url
        doc.add(new TextField(FIELD_NAME_SUBJECT, subject, Store.NO));
        doc.add(new TextField(FIELD_NAME_OBJECT, object, Store.YES));
        iwriter.addDocument(doc);
    }

    public List<Candidate> search(String queryString) throws ParseException, IOException {
        log.debug("\t start asking index...");
        List<Candidate> candidates = new ArrayList<Candidate>();
        try {
            // String replaceAll = codec.decode(queryString);// .trim().replaceAll("/", "\\\\/");
            // EscapeQuerySyntaxImpl escape = new EscapeQuerySyntaxImpl();
            // queryString = (String) escape.escape(queryString, Locale.ENGLISH, EscapeQuerySyntax.Type.STRING);
            // Query query = parser.parse(replaceAll);
            // System.out.println(query);
            // ScoreDoc[] hits = isearcher.search(query, 1000).scoreDocs;
            TermQuery tq = new TermQuery(new Term(FIELD_NAME_SUBJECT, queryString));
            // BooleanClauses Enum SHOULD says Use this operator for clauses that should appear in the matching
            // documents.
            BooleanQuery bq = new BooleanQuery();
            bq.add(tq, BooleanClause.Occur.SHOULD);
            TopScoreDocCollector collector = TopScoreDocCollector.create(1000, true);
            isearcher.search(bq, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;

            for (int i = 0; i < hits.length; i++) {
                Document hitDoc = isearcher.doc(hits[i].doc);
                // watch out, it is ensured that only documents with the baseURI go into index
                String url = hitDoc.get(FIELD_NAME_OBJECT);
                log.debug("\t Candidate: " + url);
                candidates.add(new Candidate(url, queryString, null));
            }
            log.debug("\t finished asking index...");
        } catch (Exception e)
        {
            log.error(e.getLocalizedMessage() + " -> " + queryString);
        }
        return candidates;
    }

    public void close() throws IOException {
        ireader.close();
        directory.close();
    }

    private class OnlineStatementHandler extends RDFHandlerBase {
        @Override
        public void handleStatement(Statement st) {
            try {
                String subject = st.getSubject().stringValue();
                String object = st.getObject().stringValue();
                addDocumentToIndex(iwriter, subject, object);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
