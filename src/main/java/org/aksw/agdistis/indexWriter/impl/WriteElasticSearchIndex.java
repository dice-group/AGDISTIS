package org.aksw.agdistis.indexWriter.impl;

import org.aksw.agdistis.indexWriter.WriteIndex;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class WriteElasticSearchIndex implements WriteIndex {
    RestHighLevelClient client;
    BulkProcessor bulkProcessor;
    private String index;
    public WriteElasticSearchIndex()throws IOException{
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
        index=defaultIndex;
    }
    @Override
    public void createIndex() {
        try{
            XContentBuilder settingsBuilder = null;
                settingsBuilder = jsonBuilder()
                        .startObject()
                        .startObject("analysis")
                        .startObject("analyzer")
                        .startObject("literal_analyzer")
                        .field("type","custom")
                        .field("tokenizer", "lowercase")
                        .field("filter","asciifolding")
                        .endObject()
                        .endObject()
                        .endObject()
                        .endObject();


            XContentBuilder mappingsBuilder =jsonBuilder()
                    .startObject()
                    .startObject("properties")
                    .startObject("subject")
                    .field("type", "keyword")
                    .endObject()
                    .startObject("predicate")
                    .field("type", "keyword")
                    .endObject()
                    .startObject("object_uri")
                    .field("type", "keyword")
                    .endObject()
                    .startObject("object_literal")
                    .field("type", "text")
                    .field("analyzer", "standard")
                    //.field("analyzer", "literal_analyzer")
                    .endObject()
                    .endObject()
                    .endObject();


            System.out.println(Strings.toString(settingsBuilder));
            CreateIndexRequest request = new CreateIndexRequest(index);
            request.mapping("_doc", mappingsBuilder);
            request.settings(settingsBuilder);
            CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);
            System.out.println(createIndexResponse);
            BulkProcessor.Listener listener = new BulkProcessor.Listener() {
                @Override
                public void beforeBulk(long executionId, BulkRequest request) {
                    System.out.println("before bulk");
                }

                @Override
                public void afterBulk(long executionId, BulkRequest request,
                                      BulkResponse response) {
                    System.out.println("after bulk1");
                }

                @Override
                public void afterBulk(long executionId, BulkRequest request,
                                      Throwable failure) {
                    System.out.println("after bulk2");
                }
            };
            BulkProcessor.Builder builder = BulkProcessor.builder(
                    (req, bulkListener) ->
                            client.bulkAsync(req, RequestOptions.DEFAULT, bulkListener),
                    listener);
            builder.setBulkActions(500);
            builder.setBulkSize(new ByteSizeValue(1L, ByteSizeUnit.MB));
            builder.setConcurrentRequests(0);
            builder.setFlushInterval(TimeValue.timeValueSeconds(10L));
            builder.setBackoffPolicy(BackoffPolicy
                    .constantBackoff(TimeValue.timeValueSeconds(1L), 3));
            bulkProcessor = BulkProcessor.builder(
                    (req, bulkListener) ->
                            client.bulkAsync(req, RequestOptions.DEFAULT, bulkListener),
                    listener).build();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void indexDocument(String subject, String predicate, String object, boolean isUri) throws IOException {
        IndexRequest indexRequest;
        if(isUri) {
            indexRequest = new IndexRequest(index, "_doc")
                    .source(jsonBuilder()
                            .startObject()
                            .field("subject", subject)
                            .field("predicate", predicate)
                            .field("object_uri", object)
                            .endObject()
                    );
        }
        else{
            indexRequest = new IndexRequest(index, "_doc")
                    .source(jsonBuilder()
                            .startObject()
                            .field("subject", subject)
                            .field("predicate", predicate)
                            .field("object_literal", object)
                            .endObject()
                    );
        }
        bulkProcessor.add(indexRequest);
    }

    @Override
    public void commit() {


    }

    @Override
    public void close() {
        try {
            if(bulkProcessor!=null)
                bulkProcessor.close();
            if(client!=null)
                client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
