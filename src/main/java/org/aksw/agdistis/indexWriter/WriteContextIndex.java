package org.aksw.agdistis.indexWriter;

import org.aksw.agdistis.util.ContextDocument;
import org.aksw.agdistis.util.Triple;

import java.io.IOException;
import java.util.List;

public interface WriteContextIndex {
    void createIndex();
    void indexDocument(ContextDocument doc) throws IOException;
    void indexDocument(String documentUri,String context,long id)throws IOException;
    void updateDocument(String documentUri,String context,long id)throws IOException;
    void indexDocument(String documentUri,String context, List<String> surfaceForm,long id)throws IOException;
    void commit();
    void close();
}
