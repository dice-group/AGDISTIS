package org.aksw.agdistis.index.indexImpl;

import org.aksw.agdistis.index.Index;
import org.aksw.agdistis.util.Triple;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.LoggerFactory;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;


public class ElasticSearchTripleIndex implements Index {


    private org.slf4j.Logger log = LoggerFactory.getLogger(ElasticSearchTripleIndex.class);

    private static final String FIELD_NAME_SUBJECT = "subject";
    private static final String FIELD_NAME_PREDICATE = "predicate";
    private static final String FIELD_NAME_OBJECT_URI = "object_uri";
    private static final String FIELD_NAME_OBJECT_LITERAL = "object_literal";

    private UrlValidator urlValidator = new UrlValidator();
    private RestHighLevelClient client;
    String defaultIndex;
    public ElasticSearchTripleIndex()throws IOException{
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
        String defaultIndex =envDefaultIndex != null ? envDefaultIndex : prop.getProperty("default_index");
        this.defaultIndex=defaultIndex;
    }
    @Override
    public List<Triple> search(String subject, String predicate, String object) {
        int defaultMaxNumberOfDocsRetrievedFromIndex = 100;
        return search(subject, predicate, object, defaultMaxNumberOfDocsRetrievedFromIndex);
    }
    @Override
    public List<Triple> search(String subject, String predicate, String object, int maxNumberOfResults) {
        BoolQueryBuilder booleanQueryBuilder = new BoolQueryBuilder();
        List<Triple> triples = new ArrayList<>();

        try {
            if (subject != null && subject.equals("http://aksw.org/notInWiki")) {
                log.error(
                        "A subject 'http://aksw.org/notInWiki' is searched in the index. That is strange and should not happen");
            }
            if (subject != null) {
                QueryBuilder q = termQuery(FIELD_NAME_SUBJECT, subject);
                booleanQueryBuilder.must(q);
            }
            if (predicate != null) {
                QueryBuilder q = termQuery(FIELD_NAME_PREDICATE, predicate);
                booleanQueryBuilder.must(q);
            }
            if (object != null && object.length() > 0) {
                QueryBuilder bq;
                if (urlValidator.isValid(object)) {

                    bq = termQuery(FIELD_NAME_OBJECT_URI, object);
                    booleanQueryBuilder.must(bq);

                } else {
                    bq = matchQuery(FIELD_NAME_OBJECT_LITERAL,object).operator(Operator.AND);
                    booleanQueryBuilder.must(bq);

                }

            }
            triples = getFromIndex(maxNumberOfResults, booleanQueryBuilder);
            //cache.put(bq, triples);

        } catch (Exception e) {
            log.error(e.getLocalizedMessage() + " -> " + subject);
            e.printStackTrace();
        }
        return triples;
    }
    //used for context index creation
    //Search for all documents that contain a literal for a specific subject
    @Override
    public List<Triple> search(String subject,int maxNumberOfResults){
        BoolQueryBuilder booleanQueryBuilder = new BoolQueryBuilder();
        List<Triple> triples = new ArrayList<>();

        try {
            if (subject != null && subject.equals("http://aksw.org/notInWiki")) {
                log.error(
                        "A subject 'http://aksw.org/notInWiki' is searched in the index. That is strange and should not happen");
            }
            if (subject != null) {
                QueryBuilder q = termQuery(FIELD_NAME_SUBJECT, subject);
                booleanQueryBuilder.must(q);
            }
            QueryBuilder lit= existsQuery(FIELD_NAME_OBJECT_LITERAL);
            booleanQueryBuilder.must(lit);

            triples = getFromIndex(maxNumberOfResults, booleanQueryBuilder);
            //cache.put(bq, triples);

        } catch (Exception e) {
            log.error(e.getLocalizedMessage() + " -> " + subject);
            e.printStackTrace();
        }
        return triples;
    }
    private List<Triple> getFromIndex(int maxNumberOfResults, QueryBuilder bq) throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(bq);
        searchSourceBuilder.size(maxNumberOfResults);
        searchRequest.source(searchSourceBuilder);
        searchRequest.indices(defaultIndex);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHit[] hits = searchResponse.getHits().getHits();

        List<Triple> triples = new ArrayList<>();
        String s, p, o;
        for (SearchHit hit : hits) {
            Map<String, Object> sources = hit.getSourceAsMap();
            s = sources.get(FIELD_NAME_SUBJECT).toString();
            p = sources.get(FIELD_NAME_PREDICATE).toString();
            if (sources.containsKey(FIELD_NAME_OBJECT_URI))
                o = sources.get(FIELD_NAME_OBJECT_URI).toString();
            else
                o = sources.get(FIELD_NAME_OBJECT_LITERAL).toString();
            Triple triple = new Triple(s, p, o);
            triples.add(triple);
        }
        log.debug("\t finished asking index...");
        return triples;
    }



    @Override
    public void close() throws IOException {
        client.close();
    }
}
