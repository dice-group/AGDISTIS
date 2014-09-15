package org.aksw.agdistis.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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
import org.apache.lucene.util.Version;
import org.slf4j.LoggerFactory;

public class TripleIndex {
	private org.slf4j.Logger log = LoggerFactory.getLogger(TripleIndex.class);

	public static final String FIELD_NAME_SUBJECT = "subject";
	public static final String FIELD_NAME_PREDICATE = "predicate";
	public static final String FIELD_NAME_OBJECT_URI = "object_uri";
	public static final String FIELD_NAME_OBJECT_LITERAL = "object_literal";

	private int defaultMaxNumberOfDocsRetrievedFromIndex = 1000;

	private Directory directory;
	private IndexSearcher isearcher;
	private DirectoryReader ireader;
	private UrlValidator urlValidator;

	public TripleIndex() {
		try {
			this.urlValidator = new UrlValidator();

			Properties prop = new Properties();
			InputStream input = new FileInputStream("agdistis.properties");
			prop.load(input);

			String index = prop.getProperty("index");
			log.info("The index will be here: " + index);

			directory = new MMapDirectory(new File(index));
			ireader = DirectoryReader.open(directory);
			isearcher = new IndexSearcher(ireader);
		} catch (IOException e) {
			log.error("Error while opening the TripleIndex.", e);
		}
	}

	public List<Triple> search(String subject, String predicate, String object) {
		return search(subject, predicate, object, defaultMaxNumberOfDocsRetrievedFromIndex);
	}

	public List<Triple> search(String subject, String predicate, String object, int maxNumberOfResults) {
		List<Triple> triples = new ArrayList<Triple>();
		BooleanQuery bq = new BooleanQuery();
		try {
			log.debug("\t start asking index...");
			if (subject != null && subject.equals("http://aksw.org/notInWiki")) {
				log.error("A subject 'http://aksw.org/notInWiki' is searched in the index. That is strange and should not happen");
			}
			if (subject != null) {
				TermQuery tq = new TermQuery(new Term(FIELD_NAME_SUBJECT, subject));
				bq.add(tq, BooleanClause.Occur.MUST);
			}
			if (predicate != null) {
				TermQuery tq = new TermQuery(new Term(FIELD_NAME_PREDICATE, predicate));
				bq.add(tq, BooleanClause.Occur.MUST);
			}
			if (object != null) {
				Query q = null;
				if (urlValidator.isValid(object)) {
					q = new TermQuery(new Term(FIELD_NAME_OBJECT_URI, object));
				} else {
					Analyzer analyzer = new LiteralAnalyzer(Version.LUCENE_44);
					QueryParser parser = new QueryParser(Version.LUCENE_44, FIELD_NAME_OBJECT_LITERAL, analyzer);
					parser.setDefaultOperator(QueryParser.Operator.OR);
					q = parser.parse(QueryParserBase.escape(object));
				}
				bq.add(q, BooleanClause.Occur.MUST);
			}

			TopScoreDocCollector collector = TopScoreDocCollector.create(maxNumberOfResults, true);
			isearcher.search(bq, collector);
			ScoreDoc[] hits = collector.topDocs().scoreDocs;

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
		} catch (Exception e) {
			log.error(e.getLocalizedMessage() + " -> " + subject);
		}
		return triples;
	}

	public void close() {
		try {
			ireader.close();
			directory.close();
		} catch (IOException e) {
			log.error(e.getLocalizedMessage());
		}
	}

	public DirectoryReader getIreader() {
		return ireader;
	}

}