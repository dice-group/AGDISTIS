package org.aksw.agdistis.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
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
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.Version;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class TripleIndex {

	private static final Version LUCENE_941 = Version.LUCENE_9_4_1;

	private org.slf4j.Logger log = LoggerFactory.getLogger(TripleIndex.class);

	public static final String FIELD_NAME_SUBJECT = "subject";
	public static final String FIELD_NAME_PREDICATE = "predicate";
	public static final String FIELD_NAME_OBJECT_URI = "object_uri";
	public static final String FIELD_NAME_OBJECT_LITERAL = "object_literal";
	public static final String FIELD_FREQ = "freq";

	private int defaultMaxNumberOfDocsRetrievedFromIndex = 100;

	private Directory directory;
	private IndexSearcher isearcher;
	private DirectoryReader ireader;
	private UrlValidator urlValidator;
	private Cache<BooleanQuery, List<Triple>> cache;
	StringUtils isInt = new StringUtils();

	public TripleIndex() throws IOException {
		Properties prop = new Properties();
		InputStream input = TripleIndex.class.getResourceAsStream("/config/agdistis.properties");
		prop.load(input);

		String envIndex = System.getenv("AGDISTIS_INDEX");
		String index = envIndex != null ? envIndex : prop.getProperty("index");
		log.info("The index will be here: " + index);
		Path path = Paths.get(index);
		directory = new MMapDirectory(path);
		ireader = DirectoryReader.open(directory);
		isearcher = new IndexSearcher(ireader);
		this.urlValidator = new UrlValidator();

		cache = CacheBuilder.newBuilder().maximumSize(50000).build();
	}
	
	public void setIndex(String index) throws IOException {
		directory = new MMapDirectory(Paths.get(index));
		ireader = DirectoryReader.open(directory);
		isearcher = new IndexSearcher(ireader);
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
				Query tq = new TermQuery(new Term(FIELD_NAME_SUBJECT, subject));
				booleanQueryBuilder.add(new BooleanClause(tq, BooleanClause.Occur.MUST));
			}
			if (predicate != null) {
				Query tq = new TermQuery(new Term(FIELD_NAME_PREDICATE, predicate));
				booleanQueryBuilder.add(new BooleanClause(tq, BooleanClause.Occur.MUST));
			}

			if (object != null && object.length() > 0) {
				Query q = null;
				if (urlValidator.isValid(object)) {

					q = new TermQuery(new Term(FIELD_NAME_OBJECT_URI, object));
					booleanQueryBuilder.add(new BooleanClause(q, BooleanClause.Occur.MUST));

				} else if (StringUtils.isNumeric(object)) {
					int tempInt = Integer.parseInt(object);
					// The value of buff size int is hardcoded in
					// <a href = "https://lucene.apache.org/core/4_10_0/core/constant-values.html#org.apache.lucene.util.NumericUtils.BUF_SIZE_INT">
					BytesRef bytes = new BytesRef(6);
					NumericUtils.intToSortableBytes(tempInt, new byte[6], 0);
					q = new TermQuery(new Term(FIELD_NAME_OBJECT_LITERAL, bytes.utf8ToString()));
					booleanQueryBuilder.add(new BooleanClause(q, BooleanClause.Occur.MUST));

				}
				else {
					Analyzer analyzer = new LiteralAnalyzer(LUCENE_941);
					QueryParser parser = new QueryParser(FIELD_NAME_OBJECT_LITERAL, analyzer);
					parser.setDefaultOperator(QueryParser.Operator.AND);
					q = parser.parse(QueryParserBase.escape(object));
					booleanQueryBuilder.add(new BooleanClause(q, BooleanClause.Occur.MUST));
				}
			}

			// use the cache
			triples = getFromIndex(maxNumberOfResults, booleanQueryBuilder.build());
			cache.put(booleanQueryBuilder.build(), triples);

		} catch (Exception e) {
			log.error(e.getLocalizedMessage() + " -> " + subject);
			e.printStackTrace();
		}
		return triples;
	}

	private List<Triple> getFromIndex(int maxNumberOfResults, BooleanQuery bq) throws IOException {
		log.debug("\t start asking index...");
		// I don't know about the threshold value, just for time being added totalHitsThreshold as 0
		TopScoreDocCollector collector = TopScoreDocCollector.create(maxNumberOfResults, 0);
		isearcher.search(bq, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		List<Triple> triples = new ArrayList<Triple>();
		String s, p, o;
		for (int i = 0; i < hits.length; i++) {
			Document hitDoc = isearcher.doc(hits[i].doc);
			s = hitDoc.get(FIELD_NAME_SUBJECT);
			p = hitDoc.get(FIELD_NAME_PREDICATE);
			o = hitDoc.get(FIELD_NAME_OBJECT_URI);
			if (o == null) {
				o = hitDoc.get(FIELD_NAME_OBJECT_LITERAL);
			}
			Triple triple = new Triple(s, p, o);
			triples.add(triple);
		}
		log.debug("\t finished asking index...");
		return triples;
	}

	public void close() throws IOException {
		ireader.close();
		directory.close();
	}

	public DirectoryReader getIreader() {
		return ireader;
	}

}
