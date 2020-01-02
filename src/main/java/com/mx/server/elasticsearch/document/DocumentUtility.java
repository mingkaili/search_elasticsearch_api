//package com.mx.server.elasticsearch.document;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.DeserializationFeature;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.mx.server.elasticsearch.ElasticsearchClient;
//import com.mx.server.elasticsearch.search.SearchUtility;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import org.apache.http.HttpHost;
//import org.apache.http.auth.UsernamePasswordCredentials;
//import org.apache.logging.log4j.util.Strings;
//import org.elasticsearch.ElasticsearchException;
//import org.elasticsearch.action.DocWriteRequest;
//import org.elasticsearch.action.DocWriteResponse;
//import org.elasticsearch.action.bulk.BulkRequest;
//import org.elasticsearch.action.bulk.BulkResponse;
//import org.elasticsearch.action.delete.DeleteRequest;
//import org.elasticsearch.action.delete.DeleteResponse;
//import org.elasticsearch.action.get.*;
//import org.elasticsearch.action.index.IndexRequest;
//import org.elasticsearch.action.index.IndexResponse;
//import org.elasticsearch.action.search.SearchRequest;
//import org.elasticsearch.action.search.SearchResponse;
//import org.elasticsearch.action.support.WriteRequest;
//import org.elasticsearch.action.support.replication.ReplicationResponse;
//import org.elasticsearch.action.update.UpdateRequest;
//import org.elasticsearch.action.update.UpdateResponse;
//import org.elasticsearch.client.RequestOptions;
//import org.elasticsearch.client.RestHighLevelClient;
//import org.elasticsearch.common.xcontent.XContentType;
//import org.elasticsearch.index.query.QueryBuilders;
//import org.elasticsearch.rest.RestStatus;
//import org.elasticsearch.search.builder.SearchSourceBuilder;
//import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
//
//import java.io.*;
//import java.util.*;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
///**
// * Created by lnkdwjl on 18-11-9.
// */
//public class DocumentUtility {
//    public final static ObjectMapper mapper = new ObjectMapper();
//
//    static {
//        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
//        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//    }
//
//    private RestHighLevelClient client;
//
//    public DocumentUtility(RestHighLevelClient client) {
//        this.client = client;
//    }
//
//    /**
//     * 获取文档总数
//     *
//     * @param index
//     * @param type
//     * @return
//     */
//    public long count(String index, String type) throws IOException {
//        if (null == index || "".equals(index) || null == type || "".equals(type)) {
//            Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, "Invalid parameter!");
//            return 0;
//        }
//        SearchRequest searchRequest = new SearchRequest(index);
//        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
//        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
//        searchRequest.source(searchSourceBuilder);
//        searchRequest.types(type);
//        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
//        return searchResponse.getHits().getTotalHits();
//    }
//
//    /**
//     * 通过文档ID获取文档内容
//     *
//     * @param index
//     * @param type
//     * @param id
//     * @return
//     */
//    public Map<String, Object> getDocument(String index, String type, String id) throws IOException {
//        GetRequest getRequest = new GetRequest(index, type, id);
//        return getDocument(getRequest);
//    }
//
//    public Map<String, Object> getDocument(String index, String type, String id, String routing) throws IOException {
//        GetRequest getRequest = new GetRequest(index, type, id);
//        getRequest.routing(routing);
//        return getDocument(getRequest);
//    }
//
//    /**
//     * 根据批量的id批量的查询(中间没有结果不返回)
//     *
//     * @param index
//     * @param type
//     * @param documentList
//     * @return
//     * @throws IOException
//     */
//    public List<Map<String, Object>> getDocumentList(String index, String type, List<Document> documentList) throws IOException {
//        MultiGetRequest multiGetRequest = new MultiGetRequest();
//
//        documentList.forEach(document -> {
//            multiGetRequest.add(new MultiGetRequest.Item(index, type, document.getId()).routing(document.getRouting()));
//        });
//
//        MultiGetResponse response = client.mget(multiGetRequest, RequestOptions.DEFAULT);
//
//        List<Map<String, Object>> result = new ArrayList<>();
//
//        int count = 0;
//        for (MultiGetItemResponse item : response.getResponses()) {
//            String indexTemp = item.getIndex();
//            String typeTemp = item.getType();
//            String id = item.getId();
////            Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, "第" + ++count + "条-》index:" + indexTemp + "; type:" + typeTemp + "; id:" + id);
//            if (item.getFailure() != null) {
//                Exception e = item.getFailure().getFailure();
//                ElasticsearchException ee = (ElasticsearchException) e;
//                if (ee.getMessage().contains("reason=no such index")) {
//                    Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, "查询的文档库不存在！");
//                }
//                Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, ((ElasticsearchException) e).getDetailedMessage());
//            }
//
//            GetResponse getResponse = item.getResponse();
//
//            if (getResponse.isExists()) {
//                Map<String, Object> sourceAsMap = getResponse.getSourceAsMap();
//                result.add(sourceAsMap);
//            } else {
//                Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, "第" + ++count + "条-》index:" + indexTemp + "; type:" + typeTemp + "; id:" + id);
//                Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, "没有查询到相应文档");
//            }
//        }
//
//        return result;
//    }
//
//    /**
//     * 根据批量的id批量的查询（中间没有结果也返回，占位）
//     *
//     * @param index
//     * @param type
//     * @param multiGetParams
//     * @return
//     * @throws IOException
//     */
//    public List<Document> getMultlDocument(String index, String type, List<MultiGetParam> multiGetParams) throws IOException {
//        MultiGetRequest multiGetRequest = new MultiGetRequest();
//
//        multiGetParams.forEach(multiGetParam -> {
//            MultiGetRequest.Item item = new MultiGetRequest.Item(index, type, multiGetParam.getId()).routing(multiGetParam.getRouting());
//            if (multiGetParam.getNoSource()) {
//                item.fetchSourceContext(FetchSourceContext.DO_NOT_FETCH_SOURCE);
//            } else if (null != multiGetParam.getIncludes() || null != multiGetParam.getExcludes()) {
//                FetchSourceContext fetchSourceContext = new FetchSourceContext(true, multiGetParam.getIncludes(), multiGetParam.getExcludes());
//                item.fetchSourceContext(fetchSourceContext);
//                multiGetRequest.add(item);
//            }
//        });
//
//        MultiGetResponse response = client.mget(multiGetRequest, RequestOptions.DEFAULT);
//
//        List<Document> result = new ArrayList<>();
//
//        for (MultiGetItemResponse item : response.getResponses()) {
////            Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, "第" + ++count + "条-》index:" + indexTemp + "; type:" + typeTemp + "; id:" + id);
//            if (item.getFailure() != null) {
//                Exception e = item.getFailure().getFailure();
//                ElasticsearchException ee = (ElasticsearchException) e;
//                if (ee.getMessage().contains("reason=no such index")) {
//                    Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, "查询的文档库不存在！");
//                }
//                Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, ((ElasticsearchException) e).getDetailedMessage());
//            }
//
//            Document document = new Document();
//            document.setId(item.getId());
//            document.setObjectMap(item.getResponse().getSourceAsMap());
//            document.setRouting(item.getResponse().getField("_routing") == null ? null : item.getResponse().getField("_routing").getValue());
//            result.add(document);
//        }
//
//        return result;
//    }
//
//    @Data
//    @AllArgsConstructor
//    public static class MultiGetParam {
//        private String id;
//        private String routing;
//        /**
//         * 是否不返回source
//         */
//        private Boolean noSource;
//        /**
//         * 返回包含某些字段
//         */
//        private String[] includes;
//        /**
//         * 返回不包含某些字段
//         */
//        private String[] excludes;
//    }
//
//
//    public Map<String, Object> getDocument(GetRequest getRequest) throws IOException {
//        try {
//            GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
//            if (getResponse.isExists()) {
//                return getResponse.getSourceAsMap();
//            } else {
//                Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, getRequest.index() + " " + getRequest.type() + " " + getRequest.id() + " not found!");
//            }
//        } catch (ElasticsearchException e) {
//            if (e.status() == RestStatus.NOT_FOUND) {
//                Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, getRequest.index() + " " + getRequest.type() + " " + getRequest.id() + " not found!");
//            }
//            if (e.status() == RestStatus.CONFLICT) {
//                Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, getRequest.index() + " " + getRequest.type() + " " + getRequest.id() + " version conflict!");
//            }
//            Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, e.getDetailedMessage());
//        }
//        return new HashMap<>();
//    }
//
//    /**
//     * 创建一个文档
//     *
//     * @param index
//     * @param type
//     * @param document
//     * @return
//     */
//    public boolean createDocument(String index, String type, Document document) throws IOException {
//        if (null == index || "".equals(index) || null == type || "".equals(type) || null == document) {
//            Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, "Invalid parameter!");
//            return false;
//        }
//        IndexRequest indexRequest = new IndexRequest(index, type, document.getId())
//                .source(mapper.writeValueAsString(document.getObjectMap()), XContentType.JSON);
//        if (Strings.isNotEmpty(document.getRouting())) {
//            indexRequest = indexRequest.routing(document.getRouting());
//        }
//        indexRequest.opType(DocWriteRequest.OpType.CREATE);
//        indexRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//        try {
//            IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
//            ReplicationResponse.ShardInfo shardInfo = indexResponse.getShardInfo();
//            if (shardInfo.getFailed() > 0) {
//                for (ReplicationResponse.ShardInfo.Failure failure : shardInfo.getFailures()) {
//                    String reason = failure.reason();
//                    Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, reason);
//                }
//                return false;
//            }
//        } catch (ElasticsearchException e) {
//            if (e.status() == RestStatus.CONFLICT) {
//                Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, index + " " + type + " " + document.getId() + " already exists!");
//                return false;
//            }
//            Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, e.getDetailedMessage());
//        }
//        return true;
//    }
//
//    /**
//     * 先删除再创建
//     *
//     * @param index
//     * @param type
//     * @param document
//     * @return
//     * @throws IOException
//     */
//    public boolean deleteAndCreateDocument(String index, String type, Document document) throws IOException {
//        if (null == index || "".equals(index) || null == type || "".equals(type) || null == document) {
//            Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, "Invalid parameter!");
//            return false;
//        }
//        IndexRequest indexRequest = new IndexRequest(index, type, document.getId())
//                .source(mapper.writeValueAsString(document.getObjectMap()), XContentType.JSON);
//        if (Strings.isNotEmpty(document.getRouting())) {
//            indexRequest = indexRequest.routing(document.getRouting());
//        }
//        indexRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//        try {
//            IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
//            ReplicationResponse.ShardInfo shardInfo = indexResponse.getShardInfo();
//            if (shardInfo.getFailed() > 0) {
//                for (ReplicationResponse.ShardInfo.Failure failure : shardInfo.getFailures()) {
//                    String reason = failure.reason();
//                    Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, reason);
//                }
//                return false;
//            }
//        } catch (ElasticsearchException e) {
//            if (e.status() == RestStatus.CONFLICT) {
//                Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, index + " " + type + " " + document.getId() + " already exists!");
//                return false;
//            }
//            Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, e.getDetailedMessage());
//        }
//        return true;
//    }
//
//    /**
//     * 先删除再创建批量
//     *
//     * @param index
//     * @param type
//     * @param documentList
//     * @return
//     * @throws IOException
//     */
//    public boolean deleteAndCreateDocumentBatch(String index, String type, List<Document> documentList) throws IOException {
//        if (null == index || index.isEmpty() || null == type || type.isEmpty() || null == documentList || documentList.isEmpty()) {
//            Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, "Invalid parameter!");
//            return false;
//        }
//        BulkRequest bulkRequest = new BulkRequest();
//
//        documentList.forEach(document -> {
//            IndexRequest indexRequest = null;
//            try {
//                indexRequest = new IndexRequest(index, type, document.getId())
//                        .source(mapper.writeValueAsString(document.getObjectMap()), XContentType.JSON);
//                if (null != document.getRouting() && !document.getRouting().isEmpty()) {
//                    indexRequest.routing(document.getRouting());
//                }
//            } catch (JsonProcessingException e) {
//                Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, e.getMessage());
//            }
//            if (Strings.isNotEmpty(document.getRouting())) {
//                indexRequest = indexRequest.routing(document.getRouting());
//            }
//            bulkRequest.add(indexRequest);
//        });
//        bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//
//        try {
//            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
//            if (bulkResponse.hasFailures()) {
//                Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, bulkResponse.buildFailureMessage());
//                return false;
//            }
//        } catch (ElasticsearchException e) {
//            if (e.status() == RestStatus.CONFLICT) {
//                Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, e.getDetailedMessage());
//                return false;
//            }
//            Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, e.getDetailedMessage());
//        }
//        return true;
//    }
//
//    public boolean createDocument(String index, String type, List<Document> documentList) throws Exception {
//        return createDocument(index, type, documentList, true);
//    }
//
//    /**
//     * 创建一个文档批量
//     *
//     * @param index
//     * @param type
//     * @param documentList
//     * @return
//     */
//    public boolean createDocument(String index, String type, List<Document> documentList, boolean refresh) throws Exception {
//        if (null == index || "".equals(index) || null == type || "".equals(type) || null == documentList || documentList.size() < 1) {
//            Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, "Invalid parameter!");
//            return false;
//        }
//        BulkRequest request = new BulkRequest();
//        for (int i = 0; i < documentList.size(); i++) {
//            Document document = documentList.get(i);
//            Object objectMap = document.getObjectMap();
//            IndexRequest indexRequest;
//            String id = document.getId();
//            indexRequest = new IndexRequest(index, type, id).source(mapper.writeValueAsString(objectMap), XContentType.JSON);
//            if (Strings.isNotEmpty(document.getRouting())) {
//                indexRequest = indexRequest.routing(document.getRouting());
//            }
//            indexRequest.opType(DocWriteRequest.OpType.CREATE);
//            request.add(indexRequest);
//        }
//        if (refresh) {
//            request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//        }
//        try {
//            BulkResponse bulkResponse = client.bulk(request, RequestOptions.DEFAULT);
//            if (bulkResponse.hasFailures()) {
//                Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, bulkResponse.buildFailureMessage());
//                return false;
//            }
//        } catch (ElasticsearchException e) {
//            Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, e.getDetailedMessage());
//            return false;
//        }
//        return true;
//    }
//
//    /**
//     * 更新一个文档
//     *
//     * @param index
//     * @param type
//     * @param document
//     * @return
//     */
//    public boolean updateDocument(String index, String type, Document document) throws IOException {
//        if (null == index || "".equals(index) || null == type || "".equals(type) || null == document) {
//            Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, "Invalid parameter!");
//            return false;
//        }
//        UpdateRequest request = new UpdateRequest(index, type, document.getId());
//        if (Strings.isNotEmpty(document.getRouting())) {
//            request = request.routing(document.getRouting());
//        }
//        request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//        Object objectMap = document.getObjectMap();
//        request.doc(mapper.writeValueAsString(objectMap), XContentType.JSON);
//        try {
//            UpdateResponse updateResponse = client.update(request, RequestOptions.DEFAULT);
//            return true;
//        } catch (ElasticsearchException e) {
//            if (e.status() == RestStatus.NOT_FOUND) {
//                Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, index + " " + type + " " + document.getId() + " not found!");
//            }
//            if (e.status() == RestStatus.CONFLICT) {
//                Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, index + " version conflict!");
//            }
//            Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, e.getDetailedMessage());
//        }
//        return false;
//    }
//
//    /**
//     * 按给定唯一标识插入更新
//     *
//     * @param index
//     * @param type
//     * @param uniKey
//     * @param document
//     * @param searchUtility
//     * @return
//     */
//    public boolean upsertDocument(String index, String type, String uniKey, Document document, SearchUtility searchUtility) {
//        Map<String, List<Object>> mapSearch = new HashMap<>();
//        mapSearch.put(uniKey, new ArrayList() {{
//            add(document.getId());
//        }});
//        try {
//            SearchUtility.SearchResponseObject searchResponseObject = searchUtility.terms(
//                    index, type, mapSearch, 0, 1, null);
//
//            if (searchResponseObject.getTotal() > 0) {
//                return updateDocument("cp_clean_right_test", "_doc", document);
//            } else {
//                return createDocument("cp_clean_right_test", "_doc", document);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return false;
//    }
//
//    /**
//     * 根据es自己的id插入更新
//     *
//     * @param index
//     * @param type
//     * @param document
//     * @param searchUtility
//     * @return
//     */
//    public boolean upsertDocument(String index, String type, Document document, SearchUtility searchUtility) {
//        try {
//            boolean exists = searchUtility.exists(index, type, document.getId(), document.getRouting());
//            if (exists) {
//                return updateDocument(index, type, document);
//            } else {
//                return createDocument(index, type, document);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return false;
//    }
//
//    /**
//     * 如果存在数据就更新，不存在就创建
//     *
//     * @param index
//     * @param type
//     * @param document
//     * @return
//     */
//    public boolean upsertDocument(String index, String type, Document document) {
//        if (null == index || "".equals(index) || null == type || "".equals(type) || null == document) {
//            Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, "Invalid parameter!");
//            return false;
//        }
//        UpdateRequest request = new UpdateRequest(index, type, document.getId());
//        if (Strings.isNotEmpty(document.getRouting())) {
//            request = request.routing(document.getRouting());
//        }
//
//        request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//        Object objectMap = document.getObjectMap();
//
//        try {
//            request.upsert(new IndexRequest(index, type, document.getId()).source(mapper.writeValueAsString(objectMap), XContentType.JSON));
//            request.doc(mapper.writeValueAsString(objectMap), XContentType.JSON);
//            UpdateResponse updateResponse = client.update(request, RequestOptions.DEFAULT);
//            return true;
//        } catch (Exception e) {
//            Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, e.getMessage());
//            return false;
//        }
//    }
//
//    /**
//     * 如果存在数据就更新，不存在就创建
//     *
//     * @param index
//     * @param type
//     * @param documentList
//     * @return
//     */
//    public boolean upsertDocumentBatch(String index, String type, List<Document> documentList) throws JsonProcessingException {
//        if (null == index || "".equals(index) || null == type || null == documentList || documentList.isEmpty()) {
//            Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, "Invalid parameter!");
//            return false;
//        }
//        BulkRequest bulkRequest = new BulkRequest();
//        for (int i = 0; i < documentList.size(); i++) {
//            Document document = documentList.get(i);
//            Object objectMap = document.getObjectMap();
//            String id = document.getId();
//            UpdateRequest request = new UpdateRequest(index, type, id);
//            if (Strings.isNotEmpty(document.getRouting())) {
//                request = request.routing(document.getRouting());
//            }
//            request.upsert(new IndexRequest(index, type, document.getId()).source(mapper.writeValueAsString(objectMap), XContentType.JSON));
//            request.doc(mapper.writeValueAsString(objectMap), XContentType.JSON);
//            bulkRequest.add(request);
//        }
//        bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//        try {
//            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
//            if (bulkResponse.hasFailures()) {
//                Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, bulkResponse.buildFailureMessage());
//                return false;
//            }
//        } catch (ElasticsearchException e) {
//            Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, e.getDetailedMessage());
//            return false;
//        } catch (IOException ioe) {
//            Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, ioe.getMessage());
//            return false;
//        }
//        return true;
//    }
//
//
//    /**
//     * 批量更新文档
//     *
//     * @param index
//     * @param type
//     * @param documentList
//     * @return
//     */
//    public boolean updateDocument(String index, String type, List<Document> documentList) throws IOException {
//        return updateDocument(index, type, documentList, true);
//    }
//
//    /**
//     * 批量更新文档
//     *
//     * @param index
//     * @param type
//     * @param documentList
//     * @param printLog     是否打印log
//     * @return
//     */
//    public boolean updateDocument(String index, String type, List<Document> documentList, boolean printLog) throws IOException {
//        if (null == index || "".equals(index) || null == type || "".equals(type) || documentList.isEmpty()) {
//            Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, "Invalid parameter!");
//            return false;
//        }
//        BulkRequest bulkRequest = new BulkRequest();
//        for (int i = 0; i < documentList.size(); i++) {
//            Document document = documentList.get(i);
//            Object objectMap = document.getObjectMap();
//            String id = document.getId();
//            UpdateRequest request = new UpdateRequest(index, type, id);
//            if (Strings.isNotEmpty(document.getRouting())) {
//                request = request.routing(document.getRouting());
//            }
//            request.doc(mapper.writeValueAsString(objectMap), XContentType.JSON);
//            bulkRequest.add(request);
//        }
//        bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//        try {
//            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
//            if (bulkResponse.hasFailures()) {
//                if (printLog) {
//                    Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, bulkResponse.buildFailureMessage());
//                }
//                return false;
//            }
//        } catch (ElasticsearchException e) {
//            if (printLog) {
//                Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, e.getDetailedMessage());
//            }
//            return false;
//        }
//
//        return true;
//    }
//
//    /**
//     * 删除一个文档
//     *
//     * @param index
//     * @param type
//     * @param id
//     * @param routing
//     * @return
//     */
//    public boolean deleteDocument(String index, String type, String id, String routing) throws IOException {
//        if (null == index || "".equals(index) || null == type || "".equals(type) || null == id || "".equals(id)) {
//            Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, "Invalid parameter!");
//            return false;
//        }
//        try {
//            DeleteRequest request = new DeleteRequest(index, type, id);
//            request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//            if (Strings.isNotEmpty(routing)) {
//                request = request.routing(routing);
//            }
//            DeleteResponse deleteResponse = client.delete(request, RequestOptions.DEFAULT);
//            if (deleteResponse.getResult() == DocWriteResponse.Result.NOT_FOUND) {
//                Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, index + " " + type + " " + "id" + " not found!");
//                return false;
//            }
//            ReplicationResponse.ShardInfo shardInfo = deleteResponse.getShardInfo();
//            if (shardInfo.getFailed() > 0) {
//                for (ReplicationResponse.ShardInfo.Failure failure : shardInfo.getFailures()) {
//                    String reason = failure.reason();
//                    Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, reason);
//                }
//                return false;
//            }
//        } catch (ElasticsearchException exception) {
//            if (exception.status() == RestStatus.CONFLICT) {
//                Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, index + " version conflict!");
//            }
//            Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, exception.getDetailedMessage());
//        }
//        return true;
//    }
//
//    public boolean deleteDocument(String index, String type, String id) throws IOException {
//        return deleteDocument(index, type, id, null);
//    }
//
//    /**
//     * 删除文档批量
//     *
//     * @param index
//     * @param type
//     * @param documentList
//     * @return
//     */
//    public boolean deleteDocument(String index, String type, List<Document> documentList) throws IOException {
//        return deleteDocument(index, type, documentList, true);
//    }
//
//    /**
//     * 删除文档批量
//     *
//     * @param index
//     * @param type
//     * @param documentList
//     * @param printLog     是否打印log
//     * @return
//     */
//    public boolean deleteDocument(String index, String type, List<Document> documentList, boolean printLog) throws IOException {
//        if (null == index || index.isEmpty() || null == type || type.isEmpty() || null == documentList || documentList.isEmpty()) {
//            Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, "Invalid parameter!");
//            return false;
//        }
//        BulkRequest bulkRequest = new BulkRequest();
//        for (Document document : documentList) {
//            DeleteRequest request = new DeleteRequest(index, type, document.getId());
//            if (Strings.isNotEmpty(document.getRouting())) {
//                request = request.routing(document.getRouting());
//            }
//            bulkRequest.add(request);
//        }
//        bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//        try {
//            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
//            if (bulkResponse.hasFailures()) {
//                if (printLog) {
//                    Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, bulkResponse.buildFailureMessage());
//                }
//                return false;
//            }
//        } catch (ElasticsearchException e) {
//            if (printLog) {
//                Logger.getLogger(DocumentUtility.class.getName()).log(Level.WARNING, e.getDetailedMessage());
//            }
//            return false;
//        }
//        return true;
//    }
//
//    @Data
//    @NoArgsConstructor
//    public static class Document implements Serializable {
//        private String id;
//        private String routing;
//        private Object objectMap;
//
//        public Document(String id, Object objectMap, String routing) {
//            this.id = id;
//            this.routing = routing;
//            this.objectMap = objectMap;
//        }
//
//        public Document(String id, Object objectMap) {
//            this.id = id;
//            this.objectMap = objectMap;
//        }
//
//        public Document(String id, String routing) {
//            this.id = id;
//            this.routing = routing;
//        }
//
//        public Document(String id) {
//            this.id = id;
//        }
//
//        // 深度复制
//        public Document deepClone() {
//            Document document = null;
//            try {
//                // 写入字节流
//                ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                ObjectOutputStream oos = new ObjectOutputStream(baos);
//                oos.writeObject(this);
//
//                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
//                ObjectInputStream ois = new ObjectInputStream(bais);
//                document = (Document) ois.readObject();
//            } catch (IOException e) {
//                e.printStackTrace();
//            } catch (ClassNotFoundException e) {
//                e.printStackTrace();
//            }
//            return document;
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (this == o) {
//                return true;
//            }
//
//            if (o == null || getClass() != o.getClass()) {
//                return false;
//            }
//            Document document = (Document) o;
//            return Objects.equals(id, document.id) &&
//                    Objects.equals(routing, document.routing);
//        }
//
//        @Override
//        public int hashCode() {
//            return Objects.hash(id, routing);
//        }
//    }
//
//    public static void main(String[] args) throws IOException {
//        ElasticsearchClient elasticsearchClient = new ElasticsearchClient(
//                new UsernamePasswordCredentials("elastic", "elastic"),
//                new HttpHost("192.168.3.38", 9201, "http"),
//                new HttpHost("192.168.3.38", 9202, "http"),
//                new HttpHost("192.168.3.38", 9203, "http"),
//                new HttpHost("192.168.3.38", 9204, "http"),
//                new HttpHost("192.168.3.38", 9205, "http")
//        );
//        DocumentUtility documentUtility = elasticsearchClient.getDocumentUtility();
//
//        Map<String, Object> map = new HashMap<>();
//        map.put("province", "辽宁省2");
//
//        Document document = new Document();
//        document.setId("wahaha");
//        document.setRouting("553648128");
//        document.setObjectMap(map);
//
//        documentUtility.upsertDocumentBatch("unknown_db_test2", "_doc", new ArrayList() {{
//            add(document);
//        }});
////        Map<String, Object> map = documentUtility.getDocument("dbc_data_liaoning", "_doc", "1232173_Parkopedia_120.9_31.3", "838860800");
////
////        if (map.get("baseArray") != null) {
////            List<Map<String, Object>> baseArray = (List<Map<String, Object>>) map.get("baseArray");
////            List<DocumentUtility.Document> documentList = new ArrayList<>();
////            baseArray.forEach(mapTemp -> {
////                DocumentUtility.Document document = new DocumentUtility.Document();
////                document.setObjectMap(mapTemp.get("objectMap"));
////                document.setRouting(String.valueOf(mapTemp.get("routing")));
////                document.setId(String.valueOf(mapTemp.get("id")));
////                documentList.add(document);
////            });
////            for (Document document : documentList) {
////                System.out.println(document.getId());
////            }
////        }
//    }
//}
