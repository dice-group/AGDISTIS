package org.aksw.agdistis.indexWriter.impl;

import org.aksw.agdistis.indexWriter.WriteContextIndex;
import org.aksw.agdistis.util.ContextDocument;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.aksw.agdistis.indexWriter.TripleIndexCreatorContext.*;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class WriteElasticsearchIndexContext implements WriteContextIndex {
    RestHighLevelClient client;
    BulkProcessor bulkProcessor;
    private String index;
    public WriteElasticsearchIndexContext() throws IOException{
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
                    .startObject(FIELD_NAME_URI)
                    .field("type", "keyword")
                    .endObject()
                    .startObject(FIELD_NAME_SURFACE_FORM)
                    .field("type", "text")
                    .field("analyzer", "literal_analyzer")
                    .endObject()
                    .startObject(FIELD_NAME_CONTEXT)
                    .field("type", "text")
                    .field("analyzer", "literal_analyzer")
                    .endObject()
                    .startObject(FIELD_NAME_URI_COUNT)
                    .field("type", "text")
                    .field("analyzer", "literal_analyzer")
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
                    System.out.println("starting new bulk request");
                }

                @Override
                public void afterBulk(long executionId, BulkRequest request,
                                      BulkResponse response) {
                    response.forEach(e->{
                        if(e.getFailureMessage()!=null)
                            System.out.println(e.getFailureMessage());});
                    System.out.println("Bulk Finished");
                }

                @Override
                public void afterBulk(long executionId, BulkRequest request,
                                      Throwable failure) {
                    System.out.println("after bulk2");
                }
            };
            //RequestOptions.Builder optBuilder = RequestOptions.DEFAULT.toBuilder();
            //optBuilder.addHeader("refresh","true");
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
    public void indexDocument(ContextDocument doc) throws IOException {
        IndexRequest indexRequest;

            indexRequest = new IndexRequest(index, "_doc")
                    .source(jsonBuilder()
                            .startObject()
                            .field(FIELD_NAME_URI, doc.getUri())
                            .field(FIELD_NAME_SURFACE_FORM, doc.getSurfaceForm())
                            .field(FIELD_NAME_CONTEXT, doc.getContext())
                            .field(FIELD_NAME_URI_COUNT,doc.getUriCount())
                            .endObject()
                    );

        bulkProcessor.add(indexRequest);
    }

    @Override
    public void commit() {
        bulkProcessor.flush();
    }
    public void indexDocument(String documentUri,String context, List<String> surfaceForm,long id) throws IOException {
        if(surfaceForm.isEmpty())indexDocument(documentUri,context,id);
        else {
            IndexRequest indexRequest;
            indexRequest = new IndexRequest(index, "_doc")
                    .source(jsonBuilder()
                            .startObject()
                            .field(FIELD_NAME_URI, documentUri)
                            .array(FIELD_NAME_SURFACE_FORM, surfaceForm)
                            .array(FIELD_NAME_CONTEXT, context)
                            .field(FIELD_NAME_URI_COUNT, 1)
                            .endObject()
                    );
            indexRequest.id("" + id);
            bulkProcessor.add(indexRequest);
        }
    }
    public void indexDocument(String documentUri,String context,long id) throws IOException {
        IndexRequest indexRequest;
        indexRequest = new IndexRequest(index, "_doc")
                .source(jsonBuilder()
                        .startObject()
                        .field(FIELD_NAME_URI, documentUri)
                        .array(FIELD_NAME_CONTEXT,context)
                        .field(FIELD_NAME_URI_COUNT,1)
                        .endObject()
                );
        indexRequest.id(""+id);
        bulkProcessor.add(indexRequest);
    }
    public void upsertDocument(String documentUri,String surfaceForm, List<String>context){
        if(surfaceForm ==null)upsertDocument(documentUri,context);
        String scriptString="ctx._source."+FIELD_NAME_CONTEXT+".addAll(params.context);\n"
                +"ctx._source."+FIELD_NAME_SURFACE_FORM+" .add(params.surfaceForm);\n"
                +"ctx._source."+FIELD_NAME_URI_COUNT+" += "+context.size()+";\n";
        Map<String,Object> m=new HashMap<>();
        m.put("context",context);
        m.put("surfaceForm",context);
        Script script=new Script(ScriptType.INLINE,"painless",scriptString,m);
        try {
            IndexRequest indexRequest = null;
            indexRequest = new IndexRequest("index", "type", "1")
                    .source(jsonBuilder()
                                    .startObject()
                                    .field(FIELD_NAME_URI, documentUri)
                                    .array(FIELD_NAME_SURFACE_FORM, surfaceForm)
                                    .array(FIELD_NAME_CONTEXT, context.toArray())
                                    .field(FIELD_NAME_URI_COUNT, 1)
                                    .endObject()
                    );
            indexRequest.id(documentUri);
            UpdateRequest updateRequest = new UpdateRequest(index, "_doc", documentUri)
                    .script(script)
                    .upsert(indexRequest);

            bulkProcessor.add(updateRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void upsertDocument(String documentUri,List<String> context){
        String scriptString="ctx._source."+FIELD_NAME_CONTEXT+".addAll(params.context);\n"
                +"ctx._source."+FIELD_NAME_URI_COUNT+" += "+context.size()+";\n";
        Map<String,Object> m=new HashMap<>();
        m.put("context",context);
        Script script=new Script(ScriptType.INLINE,"painless",scriptString,m);
        try {
            IndexRequest indexRequest = null;
            indexRequest = new IndexRequest("index", "type", "1")
                    .source(jsonBuilder()
                            .startObject()
                            .field(FIELD_NAME_URI, documentUri)
                            .array(FIELD_NAME_CONTEXT, context.toArray())
                            .field(FIELD_NAME_URI_COUNT, 1)
                            .endObject()
                    );
            indexRequest.id(documentUri);
            UpdateRequest updateRequest = new UpdateRequest(index, "_doc", documentUri)
                    .script(script)
                    .upsert(indexRequest);

            bulkProcessor.add(updateRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void updateDocument(String documentUri,String context,long id) throws IOException{
        String scriptString="ctx._source."+FIELD_NAME_CONTEXT+".add(params.context);\n"
                +"ctx._source."+FIELD_NAME_URI_COUNT+" += 1;\n";
        Map<String,Object> m=new HashMap<>();
        m.put("context",context);
        Script script=new Script(ScriptType.INLINE,"painless",scriptString,m);

        UpdateRequest updateRequest = new UpdateRequest(index, "_doc", ""+id)
                .script(script);
        bulkProcessor.add(updateRequest);
    }

    @Override
    public void close() {
        try {
            if(bulkProcessor!=null)
                bulkProcessor.awaitClose(10, TimeUnit.SECONDS);
            if(client!=null)
                client.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
