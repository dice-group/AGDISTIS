package org.aksw.agdistis.index;

import org.aksw.agdistis.util.ContextDocument;
import org.aksw.agdistis.util.Triple;

import java.io.IOException;
import java.util.List;

public interface ContextIndex {
    List<ContextDocument> search(String context, String surfaceForm);
    List<ContextDocument> search(String context,String surfaceForm, int maxNumberOfResults);
    void close() throws IOException;
}
