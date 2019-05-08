import org.aksw.agdistis.index.indexImpl.ContextIndex;
import org.aksw.agdistis.util.ContextDocument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class ContextIndexTest {
    Logger log = LoggerFactory.getLogger(TripleIndexTest.class);
    private ContextIndex index;

    @Before
    public void init() {
        //try {
        try {
            //index =new TripleIndex();
            index = new ContextIndex();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @After
    public void close() {
        try {
            index.close();
        } catch (IOException e) {
            log.error(
                    "Can not load index or DBpedia repository due to either wrong properties in agdistis.properties or missing index at location",
                    e);
        }
    }
    @Test
    public void test(){
        String context = "Am Golfplatz 1 Rostock, Hanse- und Universitätsstadt";
        String surfaceForm ="ARAL";
        //String uri="https://portal.limbo-project.org/address/13003-130030000-130030000000-08100-3";
        List<ContextDocument> docs= index.search(context,null);
        //System.out.println(docs.getContext());
        for(ContextDocument doc:docs) {
            System.out.println(doc.getUri() + " " + doc.getUriCount()+" ");
            doc.getContext().forEach(cont -> System.out.print(cont+" "));
        }
    }
}
