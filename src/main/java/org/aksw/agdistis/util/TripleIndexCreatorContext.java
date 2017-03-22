package org.aksw.agdistis.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
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
import info.aduna.io.FileUtil;

public class TripleIndexCreatorContext {

	private static org.slf4j.Logger log = LoggerFactory.getLogger(TripleIndexCreatorContext.class);

	public static final String N_TRIPLES = "NTriples";
	public static final String TTL = "ttl";
	public static final Version LUCENE_VERSION = Version.LUCENE_44;

	private static Analyzer urlAnalyzer;
	private static Analyzer literalAnalyzer;
	private static DirectoryReader ireader;
	private static IndexWriter iwriter;
	private static MMapDirectory directory;
	private static IndexSearcher isearcher;
	private static String nodeType;
	public static final String FIELD_NAME_CONTEXT = "CONTEXT";
	public static final String FIELD_NAME_SURFACE_FORM = "SURFACE_FORM";
	public static final String FIELD_NAME_URI = "URI";
	public static final String FIELD_NAME_URI_COUNT = "URI_COUNT";

	public static void main(String args[]) {
		if (args.length > 0) {
			log.error("TripleIndexCreator works without parameters. Please use agdistis.properties File");
			return;
		}
		try {
			log.info("For using DBpedia we suggest you downlaod the following file: " + "labels_<LANG>.ttl, "
					+ "redirects_transitive_<LANG>.ttl, " + "instance_types_<LANG>.ttl, "
					+ "mappingbased_properties_<LANG>.ttl, " + "specific_mappingbased_properties_<LANG>.ttl,"
					+ "disambiguations_<LANG>.ttl." + ""
					+ "Please download them into one folder and configure it in the agdistis.properties File."
					+ "For further information have a look at our wiki: https://github.com/AKSW/AGDISTIS/wiki");

			Properties prop = new Properties();
			InputStream input = new FileInputStream("src/main/resources/config/agdistis.properties");
			prop.load(input);

			String index = prop.getProperty("index2");
			log.info("The index will be here: " + index);
			nodeType = prop.getProperty("nodeType");
			String folder = prop.getProperty("folderWithTTLFiles");
			log.info("Getting triple data from: " + folder);
			List<File> listOfFiles = new ArrayList<File>();
			for (File file : new File(folder).listFiles()) {
				if (file.getName().endsWith("ttl")) {
					listOfFiles.add(file);
				}
			}

			String folderUpdate = prop.getProperty("folderWithTTLFiles") + "/update/";
			log.info("Getting triple data from: " + folderUpdate);
			List<File> listOfFiles2 = new ArrayList<File>();
			for (File file : new File(folderUpdate).listFiles()) {
				if (file.getName().endsWith("ttl")) {
					listOfFiles2.add(file);
				}
			}

			String baseURI = prop.getProperty("baseURI");
			log.info("Setting Base URI to: " + baseURI);

			String endpoint = prop.getProperty("endpoint");
			log.info("Setting Endpoint to: " + baseURI);

			TripleIndexCreatorContext ic = new TripleIndexCreatorContext();
			ic.createIndex(listOfFiles, index, baseURI);
			ireader = DirectoryReader.open(directory);
			isearcher = new IndexSearcher(ireader);
			ic.updateIndex(listOfFiles2, baseURI, endpoint);
			ic.close();
			log.info("Finished");
		} catch (IOException e) {
			log.error("Error while creating index. Maybe the index is corrupt now.", e);
		}
	}

	public void createIndex(List<File> files, String idxDirectory, String baseURI) {
		try {
			urlAnalyzer = new SimpleAnalyzer(LUCENE_VERSION);
			literalAnalyzer = new LiteralAnalyzer(LUCENE_VERSION);
			Map<String, Analyzer> mapping = new HashMap<String, Analyzer>();
			mapping.put(FIELD_NAME_URI, urlAnalyzer);
			mapping.put(FIELD_NAME_SURFACE_FORM, literalAnalyzer);
			mapping.put(FIELD_NAME_URI_COUNT, literalAnalyzer);
			mapping.put(FIELD_NAME_CONTEXT, literalAnalyzer);
			PerFieldAnalyzerWrapper perFieldAnalyzer = new PerFieldAnalyzerWrapper(urlAnalyzer, mapping);

			File indexDirectory = new File(idxDirectory);
			indexDirectory.mkdir();
			directory = new MMapDirectory(indexDirectory);
			IndexWriterConfig config = new IndexWriterConfig(LUCENE_VERSION, perFieldAnalyzer);
			iwriter = new IndexWriter(directory, config);
			iwriter.commit();
			for (File file : files) {
				String type = FileUtil.getFileExtension(file.getName());
				if (type.equals(TTL))
					indexTTLFile(file, baseURI);
				iwriter.commit();
			}
		} catch (Exception e) {
			log.error("Error while creating TripleIndex.", e);
		}
	}

	private void indexTTLFile(File file, String baseURI)
			throws RDFParseException, RDFHandlerException, FileNotFoundException, IOException {
		log.info("Start parsing: " + file);
		RDFParser parser = new TurtleParser();
		OnlineStatementHandler osh = new OnlineStatementHandler();
		parser.setRDFHandler(osh);
		parser.setStopAtFirstError(false);
		parser.parse(new FileReader(file), baseURI);
		log.info("Finished parsing: " + file);
	}

	private class OnlineStatementHandler extends RDFHandlerBase {
		@Override
		public void handleStatement(Statement st) {
			String subject = st.getSubject().stringValue();
			String predicate = st.getPredicate().stringValue();
			String object = st.getObject().stringValue();
			try {
				addDocumentToIndex(subject, predicate, object, st.getObject() instanceof URI);
				iwriter.commit();
				ireader = DirectoryReader.open(directory);
				isearcher = new IndexSearcher(ireader);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void addDocumentToIndex(String subject, String predicate, String object, boolean isUri) throws IOException {
		log.info("here again");
		List<Triple> triples = new ArrayList<>();

		try {
			triples = search(subject, null, null, 100);
		} catch (Exception e) {
		}
		if (triples.size() == 0) {
			Document doc = new Document();
			log.debug(subject + " " + predicate + " " + object);
			doc.add(new StringField(FIELD_NAME_URI, subject, Store.YES));
			doc.add(new TextField(FIELD_NAME_SURFACE_FORM, object, Store.YES));
			doc.add(new TextField(FIELD_NAME_URI_COUNT, "1", Store.YES));
			doc.add(new TextField(FIELD_NAME_CONTEXT, object, Store.YES));
			iwriter.addDocument(doc);
		} else {
			String docID = triples.get(0).subject;
			log.info(triples.toString());
			if (isUri) {
		    object = object.replace(nodeType, "");
		    //add SPARQL queries here!
			}
			String remainContext = triples.get(0).object.concat(" " + object);
			log.info(remainContext);
			Document hitDoc = isearcher.doc(Integer.parseInt(docID));
			Document newDoc = new Document();
			newDoc.add(new StringField(FIELD_NAME_URI, triples.get(0).predicate, Store.YES));
			newDoc.add(new TextField(FIELD_NAME_SURFACE_FORM, hitDoc.get(FIELD_NAME_SURFACE_FORM), Store.YES));
			newDoc.add(new TextField(FIELD_NAME_URI_COUNT, "1", Store.YES));
			newDoc.add(new TextField(FIELD_NAME_CONTEXT, remainContext, Store.YES));
			iwriter.updateDocument(new Term(FIELD_NAME_URI, subject), newDoc);
		}

	}

	public void updateIndex(List<File> files, String baseURI, String endpoint) {
		log.info("UpdateIndexBegin");
		try {
			for (File file : files) {
				String type = FileUtil.getFileExtension(file.getName());
				if (type.equals(TTL))
					indexTTLFile(file, baseURI);
				iwriter.commit();
			}
		} catch (Exception e) {
			log.error("Error while creating TripleIndex.", e);
		}

	}

	public List<Triple> search(String subject, String predicate, String object, int maxNumberOfResults) {
		BooleanQuery bq = new BooleanQuery();
		List<Triple> triples = new ArrayList<Triple>();
		try {
			if (subject != null && subject.equals("http://aksw.org/notInWiki")) {
				log.error(
						"A subject 'http://aksw.org/notInWiki' is searched in the index. That is strange and should not happen");
			}
			if (subject != null) {
				TermQuery tq = new TermQuery(new Term(FIELD_NAME_URI, subject));
				bq.add(tq, BooleanClause.Occur.MUST);
			}
			triples = getFromIndex(maxNumberOfResults, bq);
			if (triples == null) {
				return new ArrayList<Triple>();
			}

		} catch (Exception e) {
			log.error(e.getLocalizedMessage() + " -> " + subject);

		}
		return triples;
	}

	private List<Triple> getFromIndex(int maxNumberOfResults, BooleanQuery bq) throws IOException {
		// log.debug("\t start asking index...");

		try {
			ScoreDoc[] hits = isearcher.search(bq, null, maxNumberOfResults).scoreDocs;
			List<Triple> triples = new ArrayList<Triple>();
			String s, p, o;
			for (int i = 0; i < hits.length; i++) {
				Document hitDoc = isearcher.doc(hits[i].doc);
				s = String.valueOf(hits[i].doc);
				p = hitDoc.get(FIELD_NAME_URI);
				o = hitDoc.get(FIELD_NAME_CONTEXT);
				Triple triple = new Triple(s, p, o);
				triples.add(triple);
			}
			log.debug("\t finished asking index...");
			hits = null;
			return triples;
		} catch (Exception e) {
			return null;
		}
	}

	public void close() throws IOException {

		if (iwriter != null) {
			iwriter.close();
		}

		if (ireader != null) {
			ireader.close();
		}
		if (directory != null) {
			directory.close();
		}
	}
}
