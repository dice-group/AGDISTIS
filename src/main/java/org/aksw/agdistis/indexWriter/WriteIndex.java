package org.aksw.agdistis.indexWriter;

import java.io.IOException;

public interface WriteIndex {
    void createIndex();
    void indexDocument(String subject, String predicate, String object, boolean isUri) throws IOException;
    void commit();
    void close();

}
