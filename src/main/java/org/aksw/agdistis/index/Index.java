package org.aksw.agdistis.index;

import org.aksw.agdistis.util.Triple;

import java.io.IOException;
import java.util.List;

public interface Index {
    List<Triple> search(String subject, String predicate, String object);
    List<Triple> search(String subject, String predicate, String object, int maxNumberOfResults);
    List<Triple> search(String subject, int numberOfResults);
    void close() throws IOException;
}
