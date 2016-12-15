package org.aksw.agdistis.util;

import info.aduna.io.FileUtil;

import java.io.BufferedReader;
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

	public static void main(String args[]) {
		if (args.length > 0) {
			log.error("TripleIndexCreator works without parameters. Please use agdistis.properties File");
			return;
		}
		try {
			log.info("For using DBpedia we suggest you downlaod the following file: "
					+ "labels_<LANG>.ttl, "
					+ "redirects_transitive_<LANG>.ttl, "
					+ "instance_types_<LANG>.ttl, "
					+ "mappingbased_properties_<LANG>.ttl, "
					+ "specific_mappingbased_properties_<LANG>.ttl,"
					+ "disambiguations_<LANG>.ttl."
					+ ""
					+ "Please download them into one folder and configure it in the agdistis.properties File."
					+ "For further information have a look at our wiki: https://github.com/AKSW/AGDISTIS/wiki");

			Properties prop = new Properties();
			InputStream input = new FileInputStream("src/main/resources/config/agdistis.properties");
			prop.load(input);

			String index = prop.getProperty("index");
			log.info("The index will be here: " + index);

			String folder = prop.getProperty("folderWithTTLFiles");
			log.info("Getting triple data from: " + folder);
			List<File> listOfFiles = new ArrayList<File>();
			for (File file : new File(folder).listFiles()) {
				if (file.getName().endsWith("ttl")) {
					listOfFiles.add(file);
				}
			}

			String surfaceFormTSV = prop.getProperty("surfaceFormTSV");
			log.info("Getting surface forms from: " + surfaceFormTSV);
			File file = new File(surfaceFormTSV);
			if (file.exists()) {
				listOfFiles.add(file);
			}

			String baseURI = prop.getProperty("baseURI");
			log.info("Setting Base URI to: " + baseURI);

			TripleIndexCreator ic = new TripleIndexCreator();
			ic.createIndex(listOfFiles, index, baseURI);
			ic.close();
		} catch (IOException e) {
			log.error("Error while creating index. Maybe the index is corrupt now.", e);
		}
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
				String type = FileUtil.getFileExtension(file.getName());
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

	private void indexTTLFile(File file, String baseURI) throws RDFParseException, RDFHandlerException, FileNotFoundException, IOException {
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
				doc.add(new StringField(TripleIndex.FIELD_NAME_PREDICATE, "http://www.w3.org/2004/02/skos/core#altLabel", Store.YES));
				doc.add(new TextField(TripleIndex.FIELD_NAME_OBJECT_LITERAL, object, Store.YES));
				iwriter.addDocument(doc);
			}
		}
		br.close();
		log.info("Finished parsing: " + file);
	}

	private void addDocumentToIndex(IndexWriter iwriter, String subject, String predicate, String object, boolean isUri) throws IOException {
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

	public void close() throws IOException {
		if (ireader != null) {
			ireader.close();
		}
		if (directory != null) {
			directory.close();
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