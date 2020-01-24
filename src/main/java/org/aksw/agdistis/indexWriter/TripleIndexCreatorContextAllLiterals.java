package org.aksw.agdistis.indexWriter;

import info.aduna.io.FileUtil;
import org.aksw.agdistis.index.Index;
import org.aksw.agdistis.index.indexImpl.ElasticSearchTripleIndex;
import org.aksw.agdistis.index.indexImpl.TripleIndex;
import org.aksw.agdistis.indexWriter.impl.WriteElasticsearchIndexContext;
import org.aksw.agdistis.indexWriter.impl.WriteLuceneIndexContext;
import org.aksw.agdistis.util.ContextDocument;
import org.aksw.agdistis.util.Triple;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.ntriples.NTriplesParser;
import org.openrdf.rio.turtle.TurtleParser;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class TripleIndexCreatorContextAllLiterals {
    private static org.slf4j.Logger log = LoggerFactory.getLogger(TripleIndexCreatorContextAllLiterals.class);

    public static final String NT = "nt";
    public static final String TTL = "ttl";
    //public static final Version LUCENE_VERSION = Version.LUCENE_44;


    private static String nodeType;
    private static String baseURI;
    private static String endpoint;
    private static Index searchIndex;
    public static final String FIELD_NAME_CONTEXT = "CONTEXT";
    public static final String FIELD_NAME_SURFACE_FORM = "SURFACE_FORM";
    public static final String FIELD_NAME_URI = "URI";
    public static final String FIELD_NAME_URI_COUNT = "URI_COUNT";
    private HashMap<String,String> knownSubjects;
    private HashMap<String,Long>ids;
    WriteContextIndex index;
    private long maxId=0;

    private Map<String, ContextDocument> toIndex = new HashMap<>();
    private int count = 0;

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

            String envIndex = System.getenv("AGDISTIS_INDEX_BY_CONTEXT");
            String index = envIndex != null ? envIndex : prop.getProperty("index_bycontext");
            log.info("The index will be here: " + index);
            String envNodeType = System.getenv("AGDISTIS_NODE_TYPE");
            nodeType = envNodeType != null ? envNodeType : prop.getProperty("nodeType");
            String envFolderWithTtlFiles = System.getenv("AGDISTIS_FOLDER_WITH_TTL_FILES");
            String folder = envFolderWithTtlFiles != null ? envFolderWithTtlFiles : prop.getProperty("folderWithTTLFiles");
            log.info("Getting triple data from: " + folder);
            List<File> listOfFiles = new ArrayList<File>();
            for (File file : new File(folder).listFiles()) {
                if (file.getName().endsWith("ttl")||file.getName().endsWith("nt")) {
                    listOfFiles.add(file);
                }
            }

            String envBaseUri = System.getenv("AGDISTIS_BASE_URI");
            baseURI = envBaseUri != null ? envBaseUri : prop.getProperty("baseURI");
            log.info("Setting Base URI to: " + baseURI);
            String envIndexType = System.getenv("useElasticsearch");
            Boolean useElasticsearch = Boolean.valueOf(envIndexType != null ? envIndexType : prop.getProperty("useElasticsearch"));
            if(useElasticsearch)
            searchIndex = new ElasticSearchTripleIndex();
            else searchIndex=new TripleIndex();
            TripleIndexCreatorContextAllLiterals ic = new TripleIndexCreatorContextAllLiterals();
            ic.createIndex(listOfFiles, baseURI,index ,useElasticsearch);
            searchIndex.close();
            log.info("Finished");

        } catch (IOException e) {
            log.error("Error while creating index. Maybe the index is corrupt now.", e);
        }
    }

    public void createIndex(List<File> files, String baseURI, String idxDirectory, Boolean useElasticsearch) {
        try {
            knownSubjects=new HashMap<>();
            ids=new HashMap<String,Long>();
            if(useElasticsearch)
                index = new WriteElasticsearchIndexContext();
            else index = new WriteLuceneIndexContext(idxDirectory);
            index.createIndex();
            for (File file : files) {
                System.out.print("File " + file.getName());
                String type = FileUtil.getFileExtension(file.getName());
                if (type.equals(TTL))
                    processTTLFile(file, baseURI,TTL);
                if(type.equals(NT))
                    processTTLFile(file, baseURI,NT);
                System.out.println(" .. done");
            }
            //indexing
            searchIndex.close();
            index.commit();
            index.close();
        } catch (Exception e) {
            log.error("Error while creating TripleIndex.", e);
        }
    }

    private void processTTLFile(File file, String baseURI,String type)
            throws RDFParseException, RDFHandlerException, FileNotFoundException, IOException {
        log.info("Start parsing: " + file);
        RDFParser parser;
        if(TTL.equals(type))
            parser=new TurtleParser();
        else parser=new NTriplesParser();
        TripleIndexCreatorContextAllLiterals.OnlineStatementHandler osh = new TripleIndexCreatorContextAllLiterals.OnlineStatementHandler();
        parser.setRDFHandler(osh);
        parser.setStopAtFirstError(false);
        parser.parse(new FileReader(file), baseURI);

        // for remaining triples
        pushToIndex();

        log.info("Finished parsing: " + file);
    }

    private class OnlineStatementHandler extends RDFHandlerBase {
        @Override
        public void handleStatement(Statement st){
            String subject = st.getSubject().stringValue();
            String predicate = st.getPredicate().stringValue();
            String object = st.getObject().stringValue();

            processTriple(subject, predicate, object, st.getObject() instanceof URI);
        }
    }

    private void processTriple(String subject, String predicate, String object, boolean isUri){
        count++;

        ContextDocument contextDocument = toIndex.get(subject);
        if(contextDocument == null) {
            contextDocument = new ContextDocument(subject);
            toIndex.put(subject, contextDocument);
        }

        if (!isUri) {
            if ("http://www.w3.org/2000/01/rdf-schema#label".equals(predicate)) {
                contextDocument.addContext(object);
                contextDocument.addSurfaceForm(object);

            } else {
                contextDocument.addContext(object);
            }

        } else {
            contextDocument.addObject(object);
        }

        // index once a million triples have been seen
        if(count == 1000000)
        {
            pushToIndex();
        }
    }

    private void pushToIndex()
    {
        Set<String> allObjects = new HashSet<>();
        toIndex.values().forEach(contextDocument -> allObjects.addAll(contextDocument.getObjects()));

        Map<String, List<String>> searchresults = new HashMap<>();
        allObjects.forEach(s -> {
            List<Triple> objectLiterals = searchIndex.search(s, 100);
            List<String> context = new ArrayList<>();
            if (objectLiterals.size() > 0) {
                objectLiterals.forEach(triple -> context.add(triple.getObject()));
            }
            searchresults.put(s, context);
        });

        for(String documentUri : toIndex.keySet()) {
            ContextDocument document = toIndex.get(documentUri);
            document.getObjects().forEach(object -> document.getContext().addAll(searchresults.get(object)));
            index.upsertDocument(documentUri, document.getSurfaceForm(), document.getContext());
        }
        // Reset the count and the Subject2ContextDocument map
        toIndex.clear();
        count = 0;
    }
}
