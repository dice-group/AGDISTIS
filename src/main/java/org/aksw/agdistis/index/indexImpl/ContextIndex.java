package org.aksw.agdistis.index.indexImpl;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.aksw.agdistis.util.ContextDocument;
import org.aksw.agdistis.util.LiteralAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class ContextIndex implements org.aksw.agdistis.index.ContextIndex {

    //private static final Version LUCENE44 = Version.LUCENE_44;

    private org.slf4j.Logger log = LoggerFactory.getLogger(ContextIndex.class);

    public static final String FIELD_NAME_CONTEXT = "CONTEXT";
    public static final String FIELD_NAME_SURFACE_FORM = "SURFACE_FORM";
    public static final String FIELD_NAME_URI = "URI";
    public static final String FIELD_NAME_URI_COUNT = "URI_COUNT";

    private int defaultMaxNumberOfDocsRetrievedFromIndex = 100;

    private Directory directory;
    private IndexSearcher isearcher;
    private DirectoryReader ireader;
    private Cache<BooleanQuery, List<ContextDocument>> cache;

    public ContextIndex() throws IOException {
        Properties prop = new Properties();
        InputStream input = ContextIndex.class.getResourceAsStream("/config/agdistis.properties");
        prop.load(input);

        String envIndex = System.getenv("AGDISTIS_INDEX_BY_CONTEXT");
        String index = envIndex != null ? envIndex : prop.getProperty("index_bycontext");
        log.info("The index will be here: " + index);

        directory = new MMapDirectory(new File(index).toPath());
        ireader = DirectoryReader.open(directory);
        isearcher = new IndexSearcher(ireader);

        cache = CacheBuilder.newBuilder().maximumSize(50000).build();
    }
    public ContextDocument search(String uri) {
        ContextDocument doc=null;
        try {
            ireader = DirectoryReader.open(directory);
            isearcher = new IndexSearcher(ireader);
            BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();

            if (uri != null && uri.equals("http://aksw.org/notInWiki")) {
                log.error(
                        "A subject 'http://aksw.org/notInWiki' is searched in the index. That is strange and should not happen");
            }
            if (uri != null) {
                //Query tq = new TermQuery(new Term(FIELD_NAME_SUBJECT, subject));
                //booleanQueryBuilder.add(tq, BooleanClause.Occur.MUST);
                Query tq = new TermQuery(new Term(FIELD_NAME_URI, uri));
                queryBuilder.add(tq, BooleanClause.Occur.MUST);
            }
            BooleanQuery bq = queryBuilder.build();
            doc = getFromIndex(1, bq).get(0);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage() + " -> " + uri);

        }

        return doc;
    }
    public List<ContextDocument> search(String context,String surfaceForm) {
        return search(context, surfaceForm, defaultMaxNumberOfDocsRetrievedFromIndex);
    }

    public List<ContextDocument> search(String context,String surfaceForm, int maxNumberOfResults) {
        BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
        List<ContextDocument> contextDocuments = new ArrayList<>();
        try {
            if (context != null && context.equals("http://aksw.org/notInWiki")) {
                log.error(
                        "A context 'http://aksw.org/notInWiki' is searched in the index. That is strange and should not happen");
            }
            if (context != null) {
                Query q = null;
                Analyzer analyzer = new LiteralAnalyzer();
                QueryParser parser = new QueryParser(FIELD_NAME_CONTEXT, analyzer);
                parser.setDefaultOperator(QueryParser.Operator.AND);
                q = parser.parse(QueryParserBase.escape(context));
                booleanQueryBuilder.add(q, BooleanClause.Occur.MUST);
            }
            if (surfaceForm != null) {

                TermQuery tq = new TermQuery(new Term(FIELD_NAME_SURFACE_FORM, surfaceForm));
                booleanQueryBuilder.add(tq, BooleanClause.Occur.SHOULD);
            }

            // use the cache
            if (null == (contextDocuments = cache.getIfPresent(booleanQueryBuilder))) {
                BooleanQuery bq=booleanQueryBuilder.build();
                contextDocuments = getFromIndex(maxNumberOfResults, bq);
                if (contextDocuments == null) {
                    return new ArrayList<>();
                }
                cache.put(bq, contextDocuments);
            }

        } catch (Exception e) {
            log.error(e.getLocalizedMessage() + " -> " + context);

        }
        return contextDocuments;
    }

    private List<ContextDocument> getFromIndex(int maxNumberOfResults, BooleanQuery bq) throws IOException {
        log.debug("\t start asking index by context...");
        ScoreDoc[] hits = isearcher.search(bq, maxNumberOfResults).scoreDocs;

        if (hits.length == 0) {
            return new ArrayList<ContextDocument>();
        }
        List<ContextDocument> documents = new ArrayList<ContextDocument>();
        List<String> context, surfaceForm;
        String  uri,uriCount;
        for (int i = 0; i < hits.length; i++) {
            Document hitDoc = isearcher.doc(hits[i].doc);
            context = Arrays.asList(hitDoc.getValues(FIELD_NAME_CONTEXT));
            uri = hitDoc.get(FIELD_NAME_URI);
            if(hitDoc.get(FIELD_NAME_SURFACE_FORM)!=null)
                surfaceForm = Arrays.asList(hitDoc.get(FIELD_NAME_SURFACE_FORM));
            else surfaceForm=null;
            uriCount = hitDoc.get(FIELD_NAME_URI_COUNT);
            ContextDocument doc = new ContextDocument(uri, surfaceForm,context,Integer.parseInt(uriCount));
            documents.add(doc);
        }
        log.debug("\t finished asking index...");

        Collections.sort(documents);

        if (documents.size() < 500) {
            return documents.subList(0, documents.size());
        } else {
            return documents.subList(0, 500);
        }
    }

    public void close() throws IOException {
        ireader.close();
        directory.close();
    }

    public DirectoryReader getIreader() {
        return ireader;
    }

}

