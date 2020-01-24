import org.aksw.agdistis.index.indexImpl.ContextIndex;
import org.aksw.agdistis.index.indexImpl.ElasticSearchContextIndex;
import org.aksw.agdistis.util.ContextDocument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class ContextIndexTest {
    Logger log = LoggerFactory.getLogger(TripleIndexTest.class);
    private org.aksw.agdistis.index.ContextIndex index;

    @Before
    public void init() {
        try {
            Properties prop = new Properties();
            InputStream input = new FileInputStream("src/main/resources/config/agdistis.properties");
            prop.load(input);

            boolean useElasticsearch = Boolean.parseBoolean(prop.getProperty("useElasticsearch"));
            if(useElasticsearch)
                index = new ElasticSearchContextIndex();
            else index =new ContextIndex();
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
