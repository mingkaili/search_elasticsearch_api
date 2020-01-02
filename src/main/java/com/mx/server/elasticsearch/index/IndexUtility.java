//package com.mx.server.elasticsearch.index;
//
//import org.elasticsearch.ElasticsearchException;
//import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
//import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
//import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
//import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
//import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsRequest;
//import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
//import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
//import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
//import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
//import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
//import org.elasticsearch.client.RequestOptions;
//import org.elasticsearch.client.RestHighLevelClient;
//import org.elasticsearch.cluster.metadata.MappingMetaData;
//import org.elasticsearch.common.collect.ImmutableOpenMap;
//import org.elasticsearch.common.settings.Settings;
//import org.elasticsearch.rest.RestStatus;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
///**
// * Created by lnkdwjl on 18-11-8.
// * 提供简单的索引操作方法
// * 更详细的操作请参考
// * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/6.4/index.html
// */
//public class IndexUtility {
//
//    private RestHighLevelClient client;
//
//    public IndexUtility(RestHighLevelClient client) {
//        this.client = client;
//    }
//
//    /**
//     * 索引是否存在
//     *
//     * @param index
//     * @return
//     */
//    public boolean indexExists(String index) throws IOException {
//        if (null == index || "".equals(index)) {
//            return false;
//        }
//        GetIndexRequest request = new GetIndexRequest();
//        request.indices(index);
//        boolean result = client.indices().exists(request, RequestOptions.DEFAULT);
//        return result;
//    }
//
//    /**
//     * 删除索引
//     *
//     * @param index
//     * @return
//     */
//    public boolean indexDelete(String index) throws IOException {
//        if (!indexExists(index)) {
//            Logger.getLogger(IndexUtility.class.getName()).log(Level.WARNING, index + " not found!");
//            return false;
//        }
//        try {
//            DeleteIndexRequest request = new DeleteIndexRequest(index);
//            boolean result = client.indices().delete(request, RequestOptions.DEFAULT).isAcknowledged();
//            return result;
//        } catch (ElasticsearchException exception) {
//            if (exception.status() == RestStatus.NOT_FOUND) {
//                return true;
//            } else {
//                Logger.getLogger(IndexUtility.class.getName()).log(Level.WARNING, exception.getDetailedMessage());
//                return false;
//            }
//        }
//    }
//
//    /**
//     * 创建索引
//     *
//     * @param index
//     * @return
//     */
//    public boolean createIndex(String index, int count) throws IOException {
//        if (indexExists(index)) {
//            Logger.getLogger(IndexUtility.class.getName()).log(Level.WARNING, index + " already exists!");
//            return false;
//        }
//        CreateIndexRequest request = new CreateIndexRequest(index);
//        request.settings(Settings.builder().put("index.number_of_shards", count));
//        CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);
//        boolean acknowledged = createIndexResponse.isAcknowledged();
//        boolean shardsAcknowledged = createIndexResponse.isShardsAcknowledged();
//        return acknowledged && shardsAcknowledged;
//    }
//
//    public boolean createIndex(String index) throws IOException {
//        return createIndex(index, 5);
//    }
//
//    /**
//     * 设置Mapping
//     *
//     * @param index
//     * @param type
//     * @param mappings e.g {name:{"type":"text"}}
//     *                 类型请参考参考 https://www.elastic.co/guide/en/elasticsearch/reference/6.4/mapping-types.html
//     * @param dynamicTemplates
//     * @return
//     */
//    public boolean putFieldMappings(String index, String type, Map<String, Map<String, Object>> mappings, List<Map<String, Object>> dynamicTemplates) throws IOException {
//        if (!indexExists(index) || null == type || "".equals(type) || null == mappings || mappings.size() < 1) {
//            Logger.getLogger(IndexUtility.class.getName()).log(Level.WARNING, "Invalid parameter!");
//            return false;
//        }
//        PutMappingRequest request = new PutMappingRequest(index);
//        request.type(type);
//        Map<String, Object> jsonMap = new HashMap<>();
//        Map<String, Object> properties = new HashMap<>();
//        mappings.forEach((K, V) -> properties.put(K, V));
//        jsonMap.put("properties", properties);
//        if (dynamicTemplates != null) {
//            jsonMap.put("dynamic_templates", dynamicTemplates);
//        }
//        request.source(jsonMap);
//        PutMappingResponse putMappingResponse = client.indices().putMapping(request, RequestOptions.DEFAULT);
//        boolean result = putMappingResponse.isAcknowledged();
//        return result;
//    }
//
//    /**
//     * 设置Mapping
//     *
//     * @param index
//     * @param type
//     * @param mappings e.g {name:{"type":"text"}}
//     *                 类型请参考参考 https://www.elastic.co/guide/en/elasticsearch/reference/6.4/mapping-types.html
//     * @return
//     */
//    public boolean putFieldMappings(String index, String type, Map<String, Map<String, Object>> mappings) throws IOException {
//        return putFieldMappings(index, type, mappings, null);
//    }
//
//    /**
//     * 设置Mapping
//     *
//     * @param index
//     * @param type
//     * @param propertiesMappings e.g {name:{"type":"text"}}
//     *                 类型请参考参考 https://www.elastic.co/guide/en/elasticsearch/reference/6.4/mapping-types.html
//     * @param setRouting 设置路由
//     * @return
//     */
//    public boolean putFieldMappings(String index, String type, Map<String, Map<String, Object>> propertiesMappings,
//                                    boolean setRouting) throws IOException {
//        return putFieldMappings(index, type, propertiesMappings, setRouting, null);
//    }
//
//    /**
//     * 设置Mapping
//     *
//     * @param index
//     * @param type
//     * @param propertiesMappings e.g {name:{"type":"text"}}
//     *                 类型请参考参考 https://www.elastic.co/guide/en/elasticsearch/reference/6.4/mapping-types.html
//     * @param setRouting 设置路由
//     * @param dynamicTemplates
//     * @return
//     */
//    public boolean putFieldMappings(String index, String type, Map<String, Map<String, Object>> propertiesMappings,
//                                    boolean setRouting, List<Map<String, Object>> dynamicTemplates) throws IOException {
//        if (!indexExists(index) || null == type || "".equals(type) || null == propertiesMappings || propertiesMappings.size() < 1) {
//            Logger.getLogger(IndexUtility.class.getName()).log(Level.WARNING, "Invalid parameter!");
//            return false;
//        }
//        PutMappingRequest request = new PutMappingRequest(index);
//        request.type(type);
//        Map<String, Object> jsonMap = new HashMap<>();
//        Map<String, Object> properties = new HashMap<>();
//        propertiesMappings.forEach((K, V) -> properties.put(K, V));
//        jsonMap.put("properties", properties);
//        if (dynamicTemplates != null) {
//            jsonMap.put("dynamic_templates", dynamicTemplates);
//        }
//        if (setRouting) {
//            Map<String, Object> routingMap = new HashMap<>();
//            routingMap.put("required", true);
//            jsonMap.put("_routing", routingMap);
//        }
//        request.source(jsonMap);
//        PutMappingResponse putMappingResponse = client.indices().putMapping(request, RequestOptions.DEFAULT);
//        boolean result = putMappingResponse.isAcknowledged();
//        return result;
//    }
//
//    /**
//     * 返回field Mapping
//     * index 索引
//     * type 文档类型
//     * fields 字段名称
//     *
//     * @param index
//     * @param type
//     * @param fields
//     * @return
//     */
//    public List<Map<String, Object>> getFieldMappings(String index, String type, String... fields) throws IOException {
//        if (!indexExists(index) || null == type || "".equals(type) || null == fields || fields.length < 1) {
//            Logger.getLogger(IndexUtility.class.getName()).log(Level.WARNING, "Invalid parameter!");
//            return new ArrayList<>();
//        }
//        GetFieldMappingsRequest request = new GetFieldMappingsRequest();
//        request.indices(index);
//        request.types(type);
//        request.fields(fields);
//        GetFieldMappingsResponse response = client.indices().getFieldMapping(request, RequestOptions.DEFAULT);
//        final Map<String, Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetaData>>> mappings =
//                response.mappings();
//        final Map<String, GetFieldMappingsResponse.FieldMappingMetaData> typeMappings =
//                mappings.get(index).get(type);
//        String[] fieldArray = fields;
//
//        List<Map<String, Object>> fieldsMappings = new ArrayList<>(fieldArray.length);
//        for (int i = 0; i < fieldArray.length; i++) {
//            final GetFieldMappingsResponse.FieldMappingMetaData metaData =
//                    typeMappings.get(fieldArray[i]);
//            final Map<String, Object> source = metaData.sourceAsMap();
//            fieldsMappings.add(source);
//        }
//        return fieldsMappings;
//    }
//
//    /**
//     * 获取doc字段类型
//     *
//     * @param index
//     * @param type
//     * @return
//     */
//    public Map<String, Object> getMappings(String index, String type) throws Exception {
//        if (!indexExists(index) || null == type || "".equals(type)) {
//            Logger.getLogger(IndexUtility.class.getName()).log(Level.WARNING, "Invalid parameter!");
//            return new HashMap<>();
//        }
//        GetMappingsRequest request = new GetMappingsRequest();
//        request.indices(index);
//        request.types(type);
//        GetMappingsResponse getMappingResponse = client.indices().getMapping(request, RequestOptions.DEFAULT);
//        ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> allMappings = getMappingResponse.mappings();
//        MappingMetaData typeMapping = allMappings.get(index).get(type);
//        return typeMapping.sourceAsMap();
//    }
//}
