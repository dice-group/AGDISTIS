package org.aksw.agdistis.indexWriter.impl;

import org.aksw.agdistis.index.indexImpl.ContextIndex;
import org.aksw.agdistis.indexWriter.WriteContextIndex;
import org.aksw.agdistis.util.ContextDocument;
import org.aksw.agdistis.util.LiteralAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.MMapDirectory;


import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.aksw.agdistis.index.indexImpl.ElasticSearchContextIndex.FIELD_NAME_CONTEXT;
import static org.aksw.agdistis.index.indexImpl.ElasticSearchContextIndex.FIELD_NAME_SURFACE_FORM;
import static org.aksw.agdistis.indexWriter.TripleIndexCreatorContext.FIELD_NAME_URI;
import static org.aksw.agdistis.indexWriter.TripleIndexCreatorContext.FIELD_NAME_URI_COUNT;


public class WriteLuceneIndexContext implements WriteContextIndex {
    private Analyzer urlAnalyzer;
    private Analyzer literalAnalyzer;
    private IndexWriter iwriter;

    private MMapDirectory directory;
    private String index;
    private ContextIndex searchIndex;
    public WriteLuceneIndexContext(String directory){
        index=directory;
    }

    @Override
    public void createIndex() {
        try {
            urlAnalyzer = new SimpleAnalyzer();
            literalAnalyzer = new LiteralAnalyzer();
            Map<String, Analyzer> mapping = new HashMap<String, Analyzer>();
            mapping.put(FIELD_NAME_URI, urlAnalyzer);
            mapping.put(FIELD_NAME_SURFACE_FORM, literalAnalyzer);
            mapping.put(FIELD_NAME_URI_COUNT, literalAnalyzer);
            mapping.put(FIELD_NAME_CONTEXT, literalAnalyzer);
            PerFieldAnalyzerWrapper perFieldAnalyzer = new PerFieldAnalyzerWrapper(urlAnalyzer, mapping);
            File indexDirectory = new File(index);
            indexDirectory.mkdir();
            directory = new MMapDirectory(indexDirectory.toPath());
            IndexWriterConfig config = new IndexWriterConfig(perFieldAnalyzer);
            iwriter = new IndexWriter(directory, config);
            iwriter.commit();
            searchIndex=new ContextIndex();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void indexDocument(ContextDocument doc) throws IOException {
        Document newDoc = new Document();
        newDoc.add(new StringField(FIELD_NAME_URI, doc.getUri(), Field.Store.YES));
        for(String surfaceForm:doc.getSurfaceForm())
            newDoc.add(new TextField(FIELD_NAME_SURFACE_FORM, surfaceForm, Field.Store.YES));
        newDoc.add(new StringField(FIELD_NAME_URI_COUNT, ""+doc.getUriCount(), Field.Store.YES));
        for(String context:doc.getContext())
            newDoc.add(new TextField(FIELD_NAME_CONTEXT, context, Field.Store.YES));
        iwriter.addDocument(newDoc);
    }
    public void indexDocument(String documentUri,String context, List<String> surfaceForm,long id) throws IOException {
        if(surfaceForm.isEmpty())indexDocument(documentUri,context,id);
        else {
            Document newDoc = new Document();
            newDoc.add(new StringField(FIELD_NAME_URI, documentUri, Field.Store.YES));
            for(String form:surfaceForm)
                newDoc.add(new TextField(FIELD_NAME_SURFACE_FORM, form, Field.Store.YES));
            newDoc.add(new TextField(FIELD_NAME_CONTEXT, context, Field.Store.YES));
            newDoc.add(new StringField(FIELD_NAME_URI_COUNT, ""+1, Field.Store.YES));
            iwriter.addDocument(newDoc);

        }
    }
    public void indexDocument(String documentUri,String context,long id) throws IOException {
        Document newDoc = new Document();
        newDoc.add(new StringField(FIELD_NAME_URI, documentUri, Field.Store.YES));
        newDoc.add(new TextField(FIELD_NAME_CONTEXT, context, Field.Store.YES));
        newDoc.add(new StringField(FIELD_NAME_URI_COUNT, ""+1, Field.Store.YES));
        iwriter.addDocument(newDoc);
    }
    public void updateDocument(String documentUri,String context,long id) throws IOException{
        commit();

        ContextDocument docToUpdate=searchIndex.search(documentUri);
        Document newDoc = new Document();
        newDoc.add(new StringField(FIELD_NAME_URI, documentUri, Field.Store.YES));
        if (docToUpdate.getSurfaceForm() != null) {
            for (String form : docToUpdate.getSurfaceForm())
                newDoc.add(new TextField(FIELD_NAME_SURFACE_FORM, form, Field.Store.YES));
        }
        for(String cont:docToUpdate.getContext())
            newDoc.add(new TextField(FIELD_NAME_CONTEXT, cont, Field.Store.YES));
        newDoc.add(new TextField(FIELD_NAME_CONTEXT, context, Field.Store.YES));
        newDoc.add(new StringField(FIELD_NAME_URI_COUNT, ""+(docToUpdate.getUriCount()+1), Field.Store.YES));
        iwriter.updateDocument(new Term(FIELD_NAME_URI, documentUri), newDoc);
    }

    @Override
    public void commit() {
        try {
            iwriter.commit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            iwriter.close();
            searchIndex.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
