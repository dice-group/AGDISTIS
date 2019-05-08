package org.aksw.agdistis.indexWriter.impl;

import org.aksw.agdistis.index.indexImpl.TripleIndex;
import org.aksw.agdistis.indexWriter.WriteIndex;
import org.aksw.agdistis.util.LiteralAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.MMapDirectory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WriteLuceneIndex implements WriteIndex {
    private Analyzer urlAnalyzer;
    private Analyzer literalAnalyzer;
    private IndexWriter iwriter;
    private MMapDirectory directory;
    private String index;
    public WriteLuceneIndex(String directory){
        index=directory;
    }
    @Override
    public void createIndex() {
        try{
            urlAnalyzer = new SimpleAnalyzer();
            literalAnalyzer = new LiteralAnalyzer();
            Map<String, Analyzer> mapping = new HashMap<String, Analyzer>();
            mapping.put(TripleIndex.FIELD_NAME_SUBJECT, urlAnalyzer);
            mapping.put(TripleIndex.FIELD_NAME_PREDICATE, urlAnalyzer);
            mapping.put(TripleIndex.FIELD_NAME_OBJECT_URI, urlAnalyzer);
            mapping.put(TripleIndex.FIELD_NAME_OBJECT_LITERAL, literalAnalyzer);
            PerFieldAnalyzerWrapper perFieldAnalyzer = new PerFieldAnalyzerWrapper(urlAnalyzer, mapping);

            File indexDirectory = new File(index);
            indexDirectory.mkdir();

                directory = new MMapDirectory(indexDirectory.toPath());

            IndexWriterConfig config = new IndexWriterConfig( perFieldAnalyzer);
            iwriter = new IndexWriter(directory, config);
            iwriter.commit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void indexDocument(String subject, String predicate, String object, boolean isUri) throws IOException {
        Document doc = new Document();
        //log.debug(subject + " " + predicate + " " + object);
        doc.add(new StringField(TripleIndex.FIELD_NAME_SUBJECT, subject, Field.Store.YES));
        doc.add(new StringField(TripleIndex.FIELD_NAME_PREDICATE, predicate, Field.Store.YES));
        if (isUri) {
            doc.add(new StringField(TripleIndex.FIELD_NAME_OBJECT_URI, object, Field.Store.YES));
        } else {
            doc.add(new TextField(TripleIndex.FIELD_NAME_OBJECT_LITERAL, object, Field.Store.YES));
        }
            iwriter.addDocument(doc);

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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
