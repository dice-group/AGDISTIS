package org.aksw.agdistis.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.slf4j.LoggerFactory;

public class TripleIndex {
	private org.slf4j.Logger log = LoggerFactory.getLogger(TripleIndex.class);
	public String FIELD_NAME_SUBJECT = "subject";
	public String FIELD_NAME_PREDICATE = "predicate";
	public String FIELD_NAME_OBJECT = "object";
	private int numberOfDocsRetrievedFromIndex = 10000;

	private Directory directory;
	private IndexSearcher isearcher;
	private DirectoryReader ireader;
	private HashMap<String, List<Triple>> cache;

	public TripleIndex(File indexDirectory) {
		try {
			directory = new MMapDirectory(indexDirectory);
			ireader = DirectoryReader.open(directory);
			isearcher = new IndexSearcher(ireader);
			cache = new HashMap<String, List<Triple>>();
		} catch (IOException e) {
			log.error(e.getLocalizedMessage());
		}
	}

	public List<Triple> search(String subject, String predicate, String object) {
		List<Triple> triples = new ArrayList<Triple>();
		BooleanQuery bq = new BooleanQuery();
		try {
			if (cache.containsKey(subject+predicate+object)) {
				return cache.get(subject+predicate+object);
			}
			log.debug("\t start asking index...");
			if (subject != null) {
				TermQuery tq = new TermQuery(new Term(FIELD_NAME_SUBJECT, subject));
				bq.add(tq, BooleanClause.Occur.MUST);
			}
			if (predicate != null) {
				TermQuery tq = new TermQuery(new Term(FIELD_NAME_PREDICATE, predicate));
				bq.add(tq, BooleanClause.Occur.MUST);
			}
			if (object != null) {
				TermQuery tq = new TermQuery(new Term(FIELD_NAME_OBJECT, object));
				bq.add(tq, BooleanClause.Occur.MUST);
			}
			TopScoreDocCollector collector = TopScoreDocCollector.create(numberOfDocsRetrievedFromIndex, true);
			isearcher.search(bq, collector);
			ScoreDoc[] hits = collector.topDocs().scoreDocs;

			for (int i = 0; i < hits.length; i++) {
				Document hitDoc = isearcher.doc(hits[i].doc);
				String s = hitDoc.get(FIELD_NAME_SUBJECT);
				String p = hitDoc.get(FIELD_NAME_PREDICATE);
				String o = hitDoc.get(FIELD_NAME_OBJECT);
				triples.add(new Triple(s, p, o));
			}
			log.debug("\t finished asking index...");
			cache.put(subject+predicate+object, triples);
		} catch (Exception e) {
			log.warn(e.getLocalizedMessage() + " -> " + subject);
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

}
