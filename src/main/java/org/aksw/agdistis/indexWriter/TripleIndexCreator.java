package org.aksw.agdistis.indexWriter;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.aksw.agdistis.index.indexImpl.TripleIndex;
import org.aksw.agdistis.indexWriter.impl.WriteElasticSearchIndex;
import org.aksw.agdistis.indexWriter.impl.WriteLuceneIndex;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.ntriples.NTriplesParser;
import org.openrdf.rio.turtle.TurtleParser;
import org.slf4j.LoggerFactory;

import info.aduna.io.FileUtil;

public class TripleIndexCreator {
    private static org.slf4j.Logger log = LoggerFactory.getLogger(org.aksw.agdistis.indexWriter.TripleIndexCreator.class);

    public static final String N_TRIPLES = "NTriples";
    public static final String TTL = "ttl";
    public static final String NT = "nt";
    public static final String TSV = "tsv";
    private WriteIndex writeIndex;

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

            String envIndex = System.getenv("AGDISTIS_INDEX");
            String index = envIndex != null ? envIndex : prop.getProperty("index");
            log.info("The index will be here: " + index);

            String envFolderWithTtlFiles = System.getenv("AGDISTIS_FOLDER_WITH_TTL_FILES");
            String folder = envFolderWithTtlFiles != null ? envFolderWithTtlFiles
                    : prop.getProperty("folderWithTTLFiles");
            log.info("Getting triple data from: " + folder);
            List<File> listOfFiles = new ArrayList<File>();
            for (File file : new File(folder).listFiles()) {
                if (file.getName().endsWith("ttl")) {
                    listOfFiles.add(file);
                }
            }

            String envSurfaceFormTsv = System.getenv("AGDISTIS_SURFACE_FORM_TSV");
            String surfaceFormTSV = envSurfaceFormTsv != null ? envSurfaceFormTsv : prop.getProperty("surfaceFormTSV");
            log.info("Getting surface forms from: " + surfaceFormTSV);
            File file = new File(surfaceFormTSV);
            if (file.exists()) {
                listOfFiles.add(file);
            }

            String envBaseUri = System.getenv("AGDISTIS_BASE_URI");
            String baseURI = envBaseUri != null ? envBaseUri : prop.getProperty("baseURI");
            log.info("Setting Base URI to: " + baseURI);
            String envIndexType = System.getenv("useElasticsearch");
            Boolean useElasticsearch = Boolean.valueOf(envIndexType != null ? envIndex : prop.getProperty("useElasticsearch"));
            org.aksw.agdistis.indexWriter.TripleIndexCreator ic = new org.aksw.agdistis.indexWriter.TripleIndexCreator();
            ic.createIndex(listOfFiles, index, baseURI,useElasticsearch);
            ic.close();
        } catch (IOException e) {
            log.error("Error while creating index. Maybe the index is corrupt now.", e);
        }
    }

    public void createIndex(List<File> files, String idxDirectory, String baseURI, Boolean useElaticsearch) {
        try {
            if(useElaticsearch)
                writeIndex = new WriteElasticSearchIndex();
            else writeIndex = new WriteLuceneIndex(idxDirectory);
            //writeIndex =new WriteLuceneIndex(idxDirectory);
            writeIndex.createIndex();
            for (File file : files) {
                String type = FileUtil.getFileExtension(file.getName());
                if (type.equals(TTL))
                    indexTTLFile(file, baseURI,TTL);
                if(type.equals(NT))
                    indexTTLFile(file, baseURI,NT);
                if (type.equals(TSV))
                    indexTSVFile(file);
                writeIndex.commit();
            }
            writeIndex.close();
        } catch (Exception e) {
            log.error("Error while creating TripleIndex.", e);
        }
    }
    void close(){
        writeIndex.close();
    }

    private void indexTTLFile(File file, String baseURI,String type)
            throws RDFParseException, RDFHandlerException, FileNotFoundException, IOException {
        log.info("Start parsing: " + file);
        RDFParser parser;
        if(TTL.equals(type))
                parser= new TurtleParser();
        else parser=new NTriplesParser();
        org.aksw.agdistis.indexWriter.TripleIndexCreator.OnlineStatementHandler osh = new org.aksw.agdistis.indexWriter.TripleIndexCreator.OnlineStatementHandler();
        parser.setRDFHandler(osh);
        parser.setStopAtFirstError(false);
        if (baseURI == null) {
            parser.parse(new FileReader(file), "");
        } else {
            parser.parse(new FileReader(file), baseURI);
        }
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
                writeIndex.indexDocument(subject,"http://www.w3.org/2004/02/skos/core#altLabel",object,false);
            }
        }
        br.close();
        log.info("Finished parsing: " + file);
    }


    private class OnlineStatementHandler extends RDFHandlerBase {
        @Override
        public void handleStatement(Statement st) {
            String subject = st.getSubject().stringValue();
            String predicate = st.getPredicate().stringValue();
            String object = st.getObject().stringValue();
            try {
                writeIndex.indexDocument(subject, predicate, object, st.getObject() instanceof URI);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

