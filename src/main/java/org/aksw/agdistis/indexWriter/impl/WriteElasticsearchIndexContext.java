package org.aksw.agdistis.indexWriter.impl;

import com.google.common.collect.Lists;
import org.aksw.agdistis.indexWriter.WriteContextIndex;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
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
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.xcontent.XContentBuilder;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.aksw.agdistis.util.Constants.FIELD_NAME_CONTEXT;
import static org.aksw.agdistis.util.Constants.FIELD_NAME_SURFACE_FORM;
import static org.aksw.agdistis.util.Constants.FIELD_NAME_URI;
import static org.aksw.agdistis.util.Constants.FIELD_NAME_URI_COUNT;
import static org.elasticsearch.xcontent.XContentFactory.jsonBuilder;


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
        try {
            GetIndexRequest existsRequest = new GetIndexRequest();
            existsRequest.indices(index);
            boolean exists = client.indices().exists(existsRequest, RequestOptions.DEFAULT);

            if(!exists) {
                XContentBuilder settingsBuilder = null;
                settingsBuilder = jsonBuilder()
                        .startObject()
                        .startObject("index")
                        .field("number_of_shards", 5)
                        .endObject()
                        .startObject("analysis")
                        .startObject("analyzer")
                        .startObject("literal_analyzer")
                        .field("type", "custom")
                        .field("tokenizer", "lowercase")
                        .field("filter", "asciifolding")
                        .endObject()
                        .endObject()
                        .endObject()
                        .endObject();


                XContentBuilder mappingsBuilder = jsonBuilder()
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


                System.out.println("Index settings: " + Strings.toString(settingsBuilder));
                CreateIndexRequest request = new CreateIndexRequest(index);
                request.mapping("_doc", mappingsBuilder);
                request.settings(settingsBuilder);
                CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);
                System.out.println(createIndexResponse);
            }
            else{
                System.out.println(String.format("Index %s already exists", index));
            }

            BulkProcessor.Listener listener = new BulkProcessor.Listener() {
                @Override
                public void beforeBulk(long executionId, BulkRequest request) {
//                    System.out.println("starting new bulk request");
                }

                @Override
                public void afterBulk(long executionId, BulkRequest request,
                                      BulkResponse response) {
                    response.forEach(e->{
                        if(e.getFailureMessage()!=null) {
                            System.out.println(e.getId() + " : " + e.getFailureMessage());
                            System.out.println(e.getFailure().getCause().toString());
                        }

                    });
//                    System.out.println("Bulk Finished");
                }

                @Override
                public void afterBulk(long executionId, BulkRequest request,
                                      Throwable failure) {
//                    System.out.println("after bulk2");
                }
            };
            //RequestOptions.Builder optBuilder = RequestOptions.DEFAULT.toBuilder();
            //optBuilder.addHeader("refresh","true");
            BulkProcessor.Builder builder = BulkProcessor.builder(
                    (req, bulkListener) ->
                            client.bulkAsync(req, RequestOptions.DEFAULT, bulkListener),
                    listener);
            builder.setBulkActions(5000);
            builder.setBulkSize(new ByteSizeValue(1L, ByteSizeUnit.MB));
            builder.setConcurrentRequests(0);
            builder.setFlushInterval(TimeValue.timeValueSeconds(10L));
            builder.setBackoffPolicy(BackoffPolicy
                    .constantBackoff(TimeValue.timeValueSeconds(1L), 3));

            bulkProcessor = builder.build();
        } catch (IOException e) {
            System.out.println("Bulk Processor Exception: " + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public void commit() {
        bulkProcessor.flush();
    }
    public void upsertDocument(String documentUri, List<String> surfaceForm, List<String> context) {
        if (surfaceForm.isEmpty()) {
            upsertDocument(documentUri, context);
            return;
        }
        String scriptString = "ctx._source." + FIELD_NAME_CONTEXT + ".addAll(params.context);\n"
                + "ctx._source." + FIELD_NAME_SURFACE_FORM + " .addAll(params.surfaceForm);\n"
                + "ctx._source." + FIELD_NAME_URI_COUNT + " += params.contextSize;\n";

        Map<String, Object> m = new HashMap<>();
        m.put("context", context);
        m.put("surfaceForm", surfaceForm);
        m.put("contextSize", context.size());

        Script script = new Script(ScriptType.INLINE, "painless", scriptString, m);
        try {
            IndexRequest indexRequest = null;
            indexRequest = new IndexRequest("index", "type", "1")
                    .source(jsonBuilder()
                            .startObject()
                            .field(FIELD_NAME_URI, documentUri)
                            .array(FIELD_NAME_SURFACE_FORM, surfaceForm.toArray())
                            .array(FIELD_NAME_CONTEXT, context.toArray())
                            .field(FIELD_NAME_URI_COUNT, context.size())
                            .endObject()
                    );
            indexRequest.id(documentUri);
            UpdateRequest updateRequest = new UpdateRequest(index, "_doc", documentUri)
                    .script(script)
                    .upsert(indexRequest);



            bulkProcessor.add(updateRequest);
        } catch (IOException e) {
            System.out.println("Upsert exception:" + Strings.toString(script));
            e.printStackTrace();
        }
    }

    private void upsertDocument(String documentUri,List<String> context){
        String scriptString="ctx._source."+FIELD_NAME_CONTEXT+".addAll(params.context);\n"
                +"ctx._source."+FIELD_NAME_URI_COUNT+" += params.contextSize;\n";
        Map<String,Object> m=new HashMap<>();
        m.put("context",context);
        m.put("contextSize",context.size());
        Script script=new Script(ScriptType.INLINE,"painless",scriptString,m);
        try {
            IndexRequest indexRequest = null;
            indexRequest = new IndexRequest("index", "type", "1")
                    .source(jsonBuilder()
                            .startObject()
                            .field(FIELD_NAME_URI, documentUri)
                            .array(FIELD_NAME_SURFACE_FORM, Lists.newArrayList().toArray())
                            .array(FIELD_NAME_CONTEXT, context.toArray())
                            .field(FIELD_NAME_URI_COUNT, context.size())
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
