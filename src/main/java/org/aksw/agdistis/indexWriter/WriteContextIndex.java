package org.aksw.agdistis.indexWriter;

import org.aksw.agdistis.util.ContextDocument;

import java.io.IOException;
import java.util.List;

public interface WriteContextIndex {
    void createIndex();
    void upsertDocument(String documentUri, List<String> surfaceForm, List<String> context);
    void close();

    void commit();
}
