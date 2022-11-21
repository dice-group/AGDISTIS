package org.aksw.agdistis.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class TripleIndexContext {

	private static final Version LUCENE_941 = Version.LUCENE_9_4_1;

	private org.slf4j.Logger log = LoggerFactory.getLogger(TripleIndexContext.class);

	public static final String FIELD_NAME_CONTEXT = "CONTEXT";
	public static final String FIELD_NAME_SURFACE_FORM = "SURFACE_FORM";
	public static final String FIELD_NAME_URI = "URI";
	public static final String FIELD_NAME_URI_COUNT = "URI_COUNT";

	private int defaultMaxNumberOfDocsRetrievedFromIndex = 100;

	private Directory directory;
	private IndexSearcher isearcher;
	private DirectoryReader ireader;
	private Cache<BooleanQuery, List<Triple>> cache;

	public TripleIndexContext() throws IOException {
		Properties prop = new Properties();
		InputStream input = TripleIndexContext.class.getResourceAsStream("/config/agdistis.properties");
		prop.load(input);

		String envIndex = System.getenv("AGDISTIS_INDEX_BY_CONTEXT");
		String index = envIndex != null ? envIndex : prop.getProperty("index_bycontext");
		log.info("The index will be here: " + index);

		directory = new MMapDirectory(Paths.get(index));
		ireader = DirectoryReader.open(directory);
		isearcher = new IndexSearcher(ireader);
		new UrlValidator();

		cache = CacheBuilder.newBuilder().maximumSize(50000).build();
	}

	public List<Triple> search(String subject, String predicate, String object) {
		return search(subject, predicate, object, defaultMaxNumberOfDocsRetrievedFromIndex);
	}

	public List<Triple> search(String subject, String predicate, String object, int maxNumberOfResults) {
		BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
		List<Triple> triples = new ArrayList<Triple>();
		try {
			if (subject != null && subject.equals("http://aksw.org/notInWiki")) {
				log.error(
						"A subject 'http://aksw.org/notInWiki' is searched in the index. That is strange and should not happen");
			}
			if (subject != null) {
				Query q = null;
				Analyzer analyzer = new LiteralAnalyzer(LUCENE_941);
				QueryParser parser = new QueryParser(FIELD_NAME_CONTEXT, analyzer);
				parser.setDefaultOperator(QueryParser.Operator.AND);
				q = parser.parse(QueryParserBase.escape(subject));
				booleanQueryBuilder.add(new BooleanClause(q, BooleanClause.Occur.MUST));
			}
			if (predicate != null) {

				TermQuery tq = new TermQuery(new Term(FIELD_NAME_SURFACE_FORM, predicate));
				booleanQueryBuilder.add(new BooleanClause(tq, BooleanClause.Occur.MUST));
			}
			if (object != null) {
				TermQuery tq = new TermQuery(new Term(FIELD_NAME_URI_COUNT, object));
				booleanQueryBuilder.add(new BooleanClause(tq, BooleanClause.Occur.MUST));
			}
			// use the cache
			if (null == (triples = cache.getIfPresent(booleanQueryBuilder.build()))) {
				triples = getFromIndex(maxNumberOfResults, booleanQueryBuilder.build());
				if (triples == null) {
					return new ArrayList<Triple>();
				}
				cache.put(booleanQueryBuilder.build(), triples);
			}

		} catch (Exception e) {
			log.error(e.getLocalizedMessage() + " -> " + subject);

		}
		return triples;
	}

	private List<Triple> getFromIndex(int maxNumberOfResults, BooleanQuery bq) throws IOException {
		 log.debug("\t start asking index by context...");
		ScoreDoc[] hits = isearcher.search(bq, maxNumberOfResults).scoreDocs;

		if (hits.length == 0) {
			return new ArrayList<Triple>();
		}
		List<Triple> triples = new ArrayList<Triple>();
		String s, p, o;
		for (int i = 0; i < hits.length; i++) {
			Document hitDoc = isearcher.doc(hits[i].doc);
			s = hitDoc.get(FIELD_NAME_CONTEXT);
			p = hitDoc.get(FIELD_NAME_URI);
			o = hitDoc.get(FIELD_NAME_URI_COUNT);
			Triple triple = new Triple(s, p, o);
			triples.add(triple);
		}
		log.debug("\t finished asking index...");

		Collections.sort(triples);

		if (triples.size() < 500) {
			return triples.subList(0, triples.size());
		} else {
		return triples.subList(0, 500);
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
