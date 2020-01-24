package org.aksw.agdistis.index.indexImpl;


import org.aksw.agdistis.index.ContextIndex;
import org.aksw.agdistis.util.ContextDocument;
import org.apache.http.HttpHost;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


import static org.aksw.agdistis.util.Constants.FIELD_NAME_CONTEXT;
import static org.aksw.agdistis.util.Constants.FIELD_NAME_SURFACE_FORM;
import static org.aksw.agdistis.util.Constants.FIELD_NAME_URI;
import static org.aksw.agdistis.util.Constants.FIELD_NAME_URI_COUNT;


import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

public class ElasticSearchContextIndex implements ContextIndex {
    private org.slf4j.Logger log = LoggerFactory.getLogger(ElasticSearchContextIndex.class);
    private RestHighLevelClient client;
    String defaultContextIndex;

    public ElasticSearchContextIndex()throws IOException{
        Properties prop = new Properties();
        InputStream input = null;
        input = new FileInputStream("src/main/resources/config/agdistis.properties");
        prop.load(input);
        String envHost = System.getenv("Elasticsearch_host");
        String elhost = envHost != null ? envHost : prop.getProperty("el_hostname");
        String envPort = System.getenv("Elasticsearch_host");
        int elport =Integer.valueOf(envPort != null ? envPort : prop.getProperty("el_port"));
        String envScheme = System.getenv("Elasticsearch_host");
        String scheme =envScheme != null ? envScheme : prop.getProperty("scheme");
        client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(elhost, elport, scheme)));
        String envDefaultIndex = System.getenv("Elasticsearch_host");
        String defaultIndex =envDefaultIndex != null ? envDefaultIndex : prop.getProperty("default_context_index");
        this.defaultContextIndex=defaultIndex;

    }
    @Override
    public List<ContextDocument> search(String context, String surfaceForm) {
        return search(context,surfaceForm,100);
    }

    @Override
    public List<ContextDocument> search(String context, String surfaceForm, int maxNumberOfResults) {
        BoolQueryBuilder booleanQueryBuilder = new BoolQueryBuilder();
        List<ContextDocument> contextDocuments = new ArrayList<>();
        try {
            if (context != null) {
                QueryBuilder bq= matchQuery(FIELD_NAME_CONTEXT,context).operator(Operator.AND);
                booleanQueryBuilder.must(bq);
            }
            if (surfaceForm != null) {
                QueryBuilder bq= matchQuery(FIELD_NAME_SURFACE_FORM,surfaceForm).operator(Operator.AND);
                booleanQueryBuilder.should(bq);
            }
            contextDocuments = getFromIndex(maxNumberOfResults, booleanQueryBuilder);
            return contextDocuments;

        } catch (Exception e) {
            log.error(e.getLocalizedMessage() + " -> " + context);
            e.printStackTrace();
        }
        return null;
    }
    private List<ContextDocument> getFromIndex(int maxNumberOfResults, QueryBuilder bq) throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(bq);
        searchSourceBuilder.size(maxNumberOfResults);
        searchRequest.source(searchSourceBuilder);
        searchRequest.indices(defaultContextIndex);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHit[] hits = searchResponse.getHits().getHits();

        List<ContextDocument> documents = new ArrayList<>();
        List<String>context,surfaceForm;
        String uri,uriCount;
        for (SearchHit hit : hits) {
            Map<String, Object> sources = hit.getSourceAsMap();
            context = (List<String>)sources.get(FIELD_NAME_CONTEXT);
            surfaceForm = (List<String>)sources.get(FIELD_NAME_SURFACE_FORM);
            uri=sources.get(FIELD_NAME_URI).toString();
            uriCount=sources.get(FIELD_NAME_URI_COUNT).toString();
            ContextDocument doc = new ContextDocument(uri, surfaceForm, context,Integer.parseInt(uriCount));
            documents.add(doc);
        }
        log.debug("\t finished asking index...");
        Collections.sort(documents);

        if (documents.size() < 500) {
            return documents.subList(0, documents.size());
        } else {
            return documents.subList(0, 500);
        }
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}
