//package com.mx.server.elasticsearch.search;
//
//import com.mx.server.elasticsearch.document.DocumentUtility;
//import com.mx.server.elasticsearch.document.DocumentUtility.Document;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import org.elasticsearch.action.get.GetRequest;
//import org.elasticsearch.action.search.*;
//import org.elasticsearch.client.HttpAsyncResponseConsumerFactory;
//import org.elasticsearch.client.RequestOptions;
//import org.elasticsearch.client.RestHighLevelClient;
//import org.elasticsearch.common.unit.DistanceUnit;
//import org.elasticsearch.common.unit.TimeValue;
//import org.elasticsearch.index.query.*;
//import org.elasticsearch.join.query.ParentIdQueryBuilder;
//import org.elasticsearch.search.Scroll;
//import org.elasticsearch.search.SearchHit;
//import org.elasticsearch.search.builder.SearchSourceBuilder;
//import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
//import org.elasticsearch.search.sort.SortOrder;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
///**
// * Created by lnkdwjl on 18-11-9.
// * 数据检索的超简单封装
// */
//public class SearchUtility {
//    private static final RequestOptions COMMON_OPTIONS;
//
//    static {
//        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
//        builder.setHttpAsyncResponseConsumerFactory(
//                new HttpAsyncResponseConsumerFactory.HeapBufferedResponseConsumerFactory(150 * 1024 * 1024));
//        COMMON_OPTIONS = builder.build();
//    }
//
//    private RestHighLevelClient client;
//    private List filters;
//    private static final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));
//    private static int size = 1000;
//
//    public SearchUtility(RestHighLevelClient client) {
//        this.client = client;
//    }
//
//    public void setFilters(List filters) {
//        this.filters = filters;
//    }
//
//    /**
//     * 检索数据
//     *
//     * @param index
//     * @param type
//     * @param key    检索条件的key
//     * @param value  检索条件的value
//     * @param offset
//     * @param limit
//     * @return
//     */
//    public SearchResponseObject match(String index, String type, String routing, int offset,
//                                      int limit, String key, String value) throws IOException {
//        SearchRequest searchRequest = new SearchRequest(index);
//        SearchSourceBuilder searchSourceBuilder = queryPrepare(index, type, routing, offset, limit, searchRequest);
//        if (searchSourceBuilder == null) {
//            return new SearchResponseObject(new ArrayList<>(), 0);
//        }
//
//        if (key.isEmpty()) {
//            searchSourceBuilder.query(QueryBuilders.matchAllQuery());
//        } else {
//            MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery(key, value);
//            searchSourceBuilder.query(matchQueryBuilder);
//        }
//        searchRequest.source(searchSourceBuilder);
//
//        return getResponse(searchRequest, offset, limit);
//    }
//
//    /**
//     * 查询前的操作
//     *
//     * @param index
//     * @param type
//     * @param routing
//     * @param offset
//     * @param limit
//     * @param searchRequest
//     * @return
//     */
//    private SearchSourceBuilder queryPrepare(String index, String type, String routing, int offset,
//                                             int limit, SearchRequest searchRequest) {
//        if (null == index || "".equals(index) || null == type || "".equals(type) || offset < 0 || limit < 1) {
//            Logger.getLogger(SearchUtility.class.getName()).log(Level.WARNING, "Invalid parameter!");
//            Logger.getLogger(SearchUtility.class.getName()).log(Level.WARNING, "index:" + index + " type:" + type + " offset:" + offset + " limit:" + limit);
//            return null;
//        }
//
//        searchRequest.types(type);
//        searchRequest.scroll(scroll);
//        if (routing != null && !routing.isEmpty()) {
//            searchRequest.routing(routing);
//        }
//        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
//        searchSourceBuilder.size(size);
//        return searchSourceBuilder;
//    }
//
//    /**
//     * match多个字段
//     *
//     * @param index
//     * @param type
//     * @param routing
//     * @param offset
//     * @param limit
//     * @param value
//     * @param fieldNames
//     * @return
//     * @throws IOException
//     */
//    public SearchResponseObject mutilMatch(String index, String type, String routing, int offset,
//                                           int limit, String value, String... fieldNames) throws IOException {
//
//        SearchRequest searchRequest = new SearchRequest(index);
//        SearchSourceBuilder searchSourceBuilder = queryPrepare(index, type, routing, offset, limit, searchRequest);
//        if (searchSourceBuilder == null) {
//            return new SearchResponseObject(new ArrayList<>(), 0);
//        }
//
//        if (fieldNames.length < 1 || fieldNames[0].isEmpty()) {
//            searchSourceBuilder.query(QueryBuilders.matchAllQuery());
//        } else {
//            MultiMatchQueryBuilder multiMatchQueryBuilder = QueryBuilders.multiMatchQuery(value, fieldNames);
//            searchSourceBuilder.query(multiMatchQueryBuilder);
//        }
//        searchRequest.source(searchSourceBuilder);
//
//        return getResponse(searchRequest, offset, limit);
//    }
//
//    /**
//     * 获得response对象
//     *
//     * @param searchRequest
//     * @param offset
//     * @param limit
//     * @return
//     * @throws IOException
//     */
//    private SearchResponseObject getResponse(SearchRequest searchRequest, int offset, int limit) throws IOException {
//        // 如果小于每次滚动个数，直接使用limit作为size
//        if (limit < size) {
//            searchRequest.source().size(limit);
//        }
//        SearchResponse searchResponse = client.search(searchRequest, COMMON_OPTIONS);
//        String scrollId = searchResponse.getScrollId();
//        SearchHit[] searchHits = searchResponse.getHits().getHits();
//        boolean full = false;
//        int num = 0;
//        int step;
//        List<Map<String, Object>> result = new ArrayList<>();
//        while (!full && null != searchHits && searchHits.length > 0) {
//            step = num * size + searchHits.length;
//            while (step > offset) {
//                if (result.size() < limit) {
//                    Map<String, Object> sourceAsMap = searchHits[offset - num * size].getSourceAsMap();
//                    offset++;
//                    result.add(sourceAsMap);
//                }
//                if (result.size() == limit) {
//                    full = true;
//                    break;
//                }
//            }
//            if (!full) {
//                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
//                scrollRequest.scroll(scroll);
//                searchResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
//                scrollId = searchResponse.getScrollId();
//                searchHits = searchResponse.getHits().getHits();
//                num++;
//            }
//        }
//        SearchResponseObject searchResponseObject = new SearchResponseObject(result, searchResponse.getHits().totalHits);
//        return searchResponseObject;
//    }
//
//    /**
//     * 检索数据
//     *
//     * @param index       索引
//     * @param type        文档
//     * @param keyValueMap 必须匹配的条件，为空检索所有内容
//     * @param offset      起始位置，从0开始
//     * @param limit       取多少个
//     * @param routing     路由
//     * @return
//     */
//    public SearchResponseObject terms(String index, String type, Map<String, List<Object>> keyValueMap, int offset,
//                                      int limit, String routing) throws IOException {
//        SearchRequest searchRequest = new SearchRequest(index);
//        SearchSourceBuilder searchSourceBuilder = queryPrepare(index, type, routing, offset, limit, searchRequest);
//        if (searchSourceBuilder == null) {
//            return new SearchResponseObject(new ArrayList<>(), 0);
//        }
//
//        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
//        if (null == keyValueMap || keyValueMap.size() < 1) {
//            boolQueryBuilder.must(QueryBuilders.matchAllQuery());
//        } else {
//            keyValueMap.forEach((K, V) -> {
//                TermsQueryBuilder termsQueryBuilder = new TermsQueryBuilder(K, V.toArray());
//                boolQueryBuilder.must(termsQueryBuilder);
//            });
//        }
//        if (null != filters) {
//            builderFilter().forEach(boolQueryBuilder::filter);
//        }
//        searchSourceBuilder.query(boolQueryBuilder);
//        searchRequest.source(searchSourceBuilder);
//
//        return getResponse(searchRequest, offset, limit);
//    }
//
//
//    /**
//     * 检查id存不存在
//     *
//     * @param index
//     * @param type
//     * @param id
//     * @return
//     * @throws IOException
//     */
//    public boolean exists(String index, String type, String id, String routing) throws IOException {
//        GetRequest getRequest = new GetRequest(index, type, id);
//        getRequest.routing(routing);
//        getRequest.fetchSourceContext(new FetchSourceContext(false));
//        getRequest.storedFields("_none_");
//
//        boolean exists = client.exists(getRequest, RequestOptions.DEFAULT);
//
//        return exists;
//    }
//
//    /**
//     * 检索数据
//     *
//     * @param index              索引
//     * @param type               文档
//     * @param mustKeyValueMap    必须匹配的键值对
//     * @param mustNotKeyValueMap 必须不匹配的键值对
//     * @param missingField       查询没有此字段的数据
//     * @param offset             起始位置，从0开始
//     * @param limit              取多少个
//     * @param routing            路由
//     * @return
//     * @throws IOException
//     */
//    public SearchResponseObject terms(String index, String type, Map<String, List<Object>> mustKeyValueMap,
//                                      Map<String, List<Object>> mustNotKeyValueMap, String missingField,
//                                      int offset, int limit, String routing) throws IOException {
//        return terms(index, type, mustKeyValueMap, mustNotKeyValueMap, missingField, offset, limit, routing, null, null);
//    }
//
//    /**
//     * 检索数据
//     *
//     * @param index              索引
//     * @param type               文档
//     * @param mustKeyValueMap    必须匹配的键值对
//     * @param mustNotKeyValueMap 必须不匹配的键值对
//     * @param missingField       查询没有此字段的数据
//     * @param offset             起始位置，从0开始
//     * @param limit              取多少个
//     * @param routing            路由
//     * @param sortField          排序字段
//     * @param sortOrder          升序 asc 降序desc
//     * @return
//     * @throws IOException
//     */
//    public SearchResponseObject terms(String index, String type, Map<String, List<Object>> mustKeyValueMap,
//                                      Map<String, List<Object>> mustNotKeyValueMap, String missingField,
//                                      int offset, int limit, String routing, String sortField, String sortOrder) throws IOException {
//
//        MultiParamObject multiParamObject = new MultiParamObject();
//        multiParamObject.setMustMap(mustKeyValueMap);
//        multiParamObject.setMustNotMap(mustNotKeyValueMap);
//        multiParamObject.setMissingFieldList(new ArrayList(){{add(missingField);}});
//        multiParamObject.setOffset(offset);
//        multiParamObject.setLimit(limit);
//        multiParamObject.setRouting(routing);
//        multiParamObject.setSortField(sortField);
//        multiParamObject.setSortOrder(sortOrder);
//
//        return terms(index, type, multiParamObject);
//    }
//
//    /**
//     * 检索数据
//     *
//     * @param index              索引
//     * @param type               文档
//     * @param mustKeyValueMap    必须匹配的键值对
//     * @param mustNotKeyValueMap 必须不匹配的键值对
//     * @param missingFieldList   查询没有此字段的数据
//     * @param mustExistFieldList 查询有此字段的数据
//     * @param rangeFieldMap      范围查询，key:字段; value: key:大于小于符号(gt,lt,gte,lte)，value:数字
//     * @param offset             起始位置，从0开始
//     * @param limit              取多少个
//     * @param routing            路由
//     * @return
//     * @throws IOException
//     */
//    public SearchResponseObject terms(String index, String type, Map<String, List<Object>> mustKeyValueMap,
//                                      Map<String, List<Object>> mustNotKeyValueMap, List<String> missingFieldList,
//                                      List<String> mustExistFieldList, Map<String, Map<String, Integer>> rangeFieldMap,
//                                      int offset, int limit, String routing) throws IOException {
//
//        MultiParamObject multiParamObject = new MultiParamObject();
//        multiParamObject.setMustMap(mustKeyValueMap);
//        multiParamObject.setMustNotMap(mustNotKeyValueMap);
//        multiParamObject.setMissingFieldList(missingFieldList);
//        multiParamObject.setMustExistFieldList(mustExistFieldList);
//        multiParamObject.setRangeFieldMap(rangeFieldMap);
//        multiParamObject.setOffset(offset);
//        multiParamObject.setLimit(limit);
//        multiParamObject.setRouting(routing);
//
//        return terms(index, type, multiParamObject);
//    }
//
//    /**
//     * 检索数据
//     *
//     * @param index              索引
//     * @param type               文档
//     * @param multiParamObject   条件
//     * @return
//     * @throws IOException
//     */
//    public SearchResponseObject terms(String index, String type, MultiParamObject multiParamObject) throws IOException {
//        SearchRequest searchRequest = termParamHandle(index, type, multiParamObject, size);
//
//        return getResponse(searchRequest, multiParamObject.getOffset(), multiParamObject.getLimit());
//    }
//
//    /**
//     * 用正则表达式检索名称，es的正则只支持查询一个字段
//     *
//     * @param index
//     * @param type
//     * @param field
//     * @param regexp  名称的正则表达式
//     * @param offset
//     * @param limit
//     * @param routing 路由
//     * @return
//     */
//    public SearchResponseObject regexpName(String index, String type, String field, String regexp, int offset,
//                                           int limit, String routing) throws IOException {
//        SearchRequest searchRequest = new SearchRequest(index);
//        SearchSourceBuilder searchSourceBuilder = queryPrepare(index, type, routing, offset, limit, searchRequest);
//        if (searchSourceBuilder == null) {
//            return new SearchResponseObject(new ArrayList<>(), 0);
//        }
//
//        RegexpQueryBuilder regexpQueryBuilder = new RegexpQueryBuilder(field, regexp);
//        searchSourceBuilder.query(regexpQueryBuilder);
//        searchRequest.source(searchSourceBuilder);
//
//        return getResponse(searchRequest, offset, limit);
//    }
//
//    /**
//     * 处理term检索的参数
//     * @param index
//     * @param type
//     * @param multiParamObject
//     */
//    private SearchRequest termParamHandle(String index, String type, MultiParamObject multiParamObject, int size) {
//        SearchRequest searchRequest = new SearchRequest(index);
//        searchRequest.types(type);
//        if (null != multiParamObject.getRouting() && !multiParamObject.getRouting().isEmpty()) {
//            searchRequest.routing(multiParamObject.getRouting());
//        }
//
//        searchRequest.scroll(scroll);
//
//        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
////        searchSourceBuilder.from(multiParamObject.getOffset());
////        searchSourceBuilder.size(multiParamObject.getLimit());
//        searchSourceBuilder.timeout(TimeValue.timeValueMinutes(1l));
//        searchSourceBuilder.size(size);
//
//        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
//
//        // match查询
//        Map<String, List<Object>> mustMatchMap = multiParamObject.getMustMatchMap();
//        if (null != mustMatchMap && mustMatchMap.size() > 0) {
//            mustMatchMap.forEach((K, V) -> {
//                MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder(K, V.toArray());
//                boolQueryBuilder.must(matchQueryBuilder);
//            });
//        }
//
//        // must term
//        Map<String, List<Object>> mustMap = multiParamObject.getMustMap();
//        if (null == mustMap || mustMap.size() < 1) {
//            boolQueryBuilder.must(QueryBuilders.matchAllQuery());
//        } else {
//            mustMap.forEach((K, V) -> {
//                TermsQueryBuilder termsQueryBuilder = new TermsQueryBuilder(K, V.toArray());
//                boolQueryBuilder.must(termsQueryBuilder);
//            });
//        }
//
//        // or查询
//        Map<String, List<Object>> orMap = multiParamObject.getOrMap();
//        if (null != orMap && orMap.size() > 0) {
//            BoolQueryBuilder boolQueryBuilderInner = new BoolQueryBuilder();
//            orMap.forEach((K, V) -> {
//                TermsQueryBuilder termsQueryBuilder = new TermsQueryBuilder(K, V.toArray());
//                boolQueryBuilderInner.should(termsQueryBuilder);
//            });
//            boolQueryBuilder.must(boolQueryBuilderInner);
//        }
//
//        // must not term
//        Map<String, List<Object>> mustNotMap = multiParamObject.getMustNotMap();
//        if (null != mustNotMap && !mustNotMap.isEmpty()) {
//            mustNotMap.forEach((K, V) -> {
//                TermsQueryBuilder termsQueryBuilder = new TermsQueryBuilder(K, V.toArray());
//                boolQueryBuilder.mustNot(termsQueryBuilder);
//            });
//        }
//
//        // fuzzy query
//        Map<String, String> fuzzyQuery = multiParamObject.getFuzzyQuery();
//        if (null != fuzzyQuery && !fuzzyQuery.isEmpty()) {
//            fuzzyQuery.forEach((K, V) -> {
//                WildcardQueryBuilder wildcardQueryBuilder = new WildcardQueryBuilder(K, V);
//                boolQueryBuilder.must(wildcardQueryBuilder);
//            });
//        }
//
//        // range
//        Map<String, Map<String, Integer>> rangeFieldMap = multiParamObject.getRangeFieldMap();
//        if (null != rangeFieldMap && !rangeFieldMap.isEmpty()) {
//            rangeFieldMap.forEach((field, range) -> {
//                if (null != range && !range.isEmpty()) {
//                    RangeQueryBuilder rangeQueryBuilder = new RangeQueryBuilder(field);
//                    range.forEach((compareSign, num) -> {
//                        switch (compareSign) {
//                            case "gte":
//                                rangeQueryBuilder.gte(num);
//                                break;
//                            case "lte":
//                                rangeQueryBuilder.lte(num);
//                                break;
//                            case "gt":
//                                rangeQueryBuilder.gt(num);
//                                break;
//                            case "lt":
//                                rangeQueryBuilder.lt(num);
//                                break;
//                            default:
//                                break;
//                        }
//                    });
//                    boolQueryBuilder.must(rangeQueryBuilder);
//                }
//            });
//        }
//
//        if (null != multiParamObject.getRangeFieldObjectMap() && !multiParamObject.getRangeFieldObjectMap().isEmpty()) {
//            multiParamObject.getRangeFieldObjectMap().forEach((field, range) -> {
//                if (null != range && !range.isEmpty()) {
//                    RangeQueryBuilder rangeQueryBuilder = new RangeQueryBuilder(field);
//                    range.forEach((compareSign, num) -> {
//                        switch (compareSign) {
//                            case "gte":
//                                rangeQueryBuilder.gte(num);
//                                break;
//                            case "lte":
//                                rangeQueryBuilder.lte(num);
//                                break;
//                            case "gt":
//                                rangeQueryBuilder.gt(num);
//                                break;
//                            case "lt":
//                                rangeQueryBuilder.lt(num);
//                                break;
//                            default:
//                                break;
//                        }
//                    });
//                    boolQueryBuilder.must(rangeQueryBuilder);
//                }
//            });
//        }
//
//        //  must not exists
//        List<String> missingFieldList = multiParamObject.getMissingFieldList();
//        if (null != missingFieldList && !missingFieldList.isEmpty()) {
//            missingFieldList.forEach(field -> {
//                if (field == null) {
//                    return;
//                }
//                ExistsQueryBuilder existsQueryBuilder = new ExistsQueryBuilder(field);
//                boolQueryBuilder.mustNot(existsQueryBuilder);
//            });
//        }
//
//        // must exists
//        List<String> mustExistFieldList = multiParamObject.getMustExistFieldList();
//        if (null != mustExistFieldList && !mustExistFieldList.isEmpty()) {
//            mustExistFieldList.forEach(field -> {
//                if (field == null) {
//                    return;
//                }
//                ExistsQueryBuilder existsQueryBuilder = new ExistsQueryBuilder(field);
//                boolQueryBuilder.must(existsQueryBuilder);
//            });
//        }
//
//        // geo
//        if (null != multiParamObject.getFilters()) {
//            builderFilter(multiParamObject.getFilters()).forEach(boolQueryBuilder::filter);
//        }
//
//        // sort
//        if (null != multiParamObject.getSortField() && null != multiParamObject.getSortOrder()) {
//            if (multiParamObject.getSortOrder().equals("asc")) {
//                searchSourceBuilder.sort(multiParamObject.getSortField(), SortOrder.ASC);
//            } else {
//                searchSourceBuilder.sort(multiParamObject.getSortField(), SortOrder.DESC);
//            }
//        }
//
//        if (null != multiParamObject.getPreference() && !multiParamObject.getPreference().isEmpty()) {
//            searchRequest.preference(multiParamObject.getPreference());
//        }
//
//        searchSourceBuilder.query(boolQueryBuilder);
//
//        searchRequest.source(searchSourceBuilder);
//
//        return searchRequest;
//    }
//
//    /**
//     * 多任务查询
//     * 返回的顺序与查询顺序一至
//     * 有一个返回结果出错则不给结果，认为查询错误
//     *
//     * @param index
//     * @param type
//     * @param paramObjectList
//     * @return
//     */
//    public List<SearchResponseObject> multiTerms(String index, String type, List<MultiParamObject> paramObjectList) throws IOException {
//        if (null == index || "".equals(index) || null == type || "".equals(type) || null == paramObjectList || paramObjectList.size() < 1) {
//            Logger.getLogger(SearchUtility.class.getName()).log(Level.WARNING, "Invalid parameter!");
//            Logger.getLogger(SearchUtility.class.getName()).log(Level.WARNING, "index:" + index + " type:" + type + " paramObjectList:" + paramObjectList);
//            return new ArrayList<>();
//        }
//        MultiSearchRequest request = new MultiSearchRequest();
//        paramObjectList.forEach(multiParamObject -> {
//            SearchRequest searchRequest = termParamHandle(index, type, multiParamObject, multiParamObject.getLimit());
//            request.add(searchRequest);
//        });
//
//        MultiSearchResponse response = client.msearch(request, COMMON_OPTIONS);
//        List<SearchResponseObject> resultResponseList = new ArrayList<>();
//        for (int i = 0; i < response.getResponses().length; i++) {
//            MultiSearchResponse.Item item = response.getResponses()[i];
//            if (item.isFailure()) {
//                return new ArrayList<>();
//            }
//            List<Map<String, Object>> result = new ArrayList<>();
//            for (SearchHit hit : item.getResponse().getHits()) {
//                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
//                result.add(sourceAsMap);
//            }
//            SearchResponseObject searchResponseObject = new SearchResponseObject(result, item.getResponse().getHits().totalHits);
//            resultResponseList.add(searchResponseObject);
//        }
//        return resultResponseList;
//    }
//
//    /**
//     * 根据父级id获取子级列表
//     *
//     * @param index        es索引
//     * @param childType    子级类型
//     * @param parentId     父级id
//     * @param routing      路由
//     * @param missingField 只查询没有此字段的数据
//     * @return
//     */
//    public List<DocumentUtility.Document> getChildrenByParentId(String index, String childType, String parentId, String routing, String missingField) {
//        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
//        SearchRequest searchRequest = new SearchRequest(index);
//
//        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
//
//        ParentIdQueryBuilder parentIdQueryBuilder = new ParentIdQueryBuilder(childType, parentId);
//        boolQueryBuilder.must(parentIdQueryBuilder);
//
//        if (null != missingField && !missingField.isEmpty()) {
//            ExistsQueryBuilder existsQueryBuilder = new ExistsQueryBuilder(missingField);
//            boolQueryBuilder.mustNot(existsQueryBuilder);
//        }
//
//        sourceBuilder.query(boolQueryBuilder);
//
//        searchRequest.source(sourceBuilder);
//        searchRequest.routing(routing);
//
//
//        SearchResponse searchResponse = null;
//        try {
//            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        List<DocumentUtility.Document> documentList = new ArrayList<>();
//        if (searchResponse == null || searchResponse.getHits() == null) {
//            return new ArrayList<>();
//        }
//
//        for (SearchHit documentFields : searchResponse.getHits().getHits()) {
//            Map<String, Object> map = documentFields.getSourceAsMap();
//
//            DocumentUtility.Document document = new DocumentUtility.Document();
//            document.setId(documentFields.getId());
//            document.setObjectMap(map);
//            document.setRouting(documentFields.getFields().get("_routing").getValue());
//            documentList.add(document);
//        }
//
//        return documentList;
//    }
//
//    /**
//     * 根据父级id获取子级列表
//     *
//     * @param index        es索引
//     * @param childType    子级类型
//     * @param missingField 只查询没有此字段的数据
//     * @param parentIdAndRoutings parentId和路由的集合
//     * @return
//     */
//    public List<List<Document>> getChildrenByParentIdBatch(String index, String childType, String missingField, List<ParentIdAndRouting> parentIdAndRoutings) {
//        MultiSearchRequest multiSearchRequest = new MultiSearchRequest();
//
//        if (parentIdAndRoutings == null || parentIdAndRoutings.isEmpty()) {
//            return new ArrayList<>();
//        }
//
//        parentIdAndRoutings.forEach(parentIdAndRouting -> {
//            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
//            BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
//
//            ParentIdQueryBuilder parentIdQueryBuilder = new ParentIdQueryBuilder(childType, parentIdAndRouting.getParentId());
//            boolQueryBuilder.must(parentIdQueryBuilder);
//
//            if (null != missingField && !missingField.isEmpty()) {
//                ExistsQueryBuilder existsQueryBuilder = new ExistsQueryBuilder(missingField);
//                boolQueryBuilder.mustNot(existsQueryBuilder);
//            }
//
//            sourceBuilder.query(boolQueryBuilder);
//
//            SearchRequest searchRequest = new SearchRequest(index);
//            searchRequest.source(sourceBuilder);
//            searchRequest.routing(parentIdAndRouting.getRouting());
//
//            multiSearchRequest.add(searchRequest);
//        });
//
//        MultiSearchResponse searchResponse = null;
//        try {
//            searchResponse = client.msearch(multiSearchRequest, RequestOptions.DEFAULT);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        List<List<Document>> result = new ArrayList<>();
//
//        if (searchResponse == null) {
//            return result;
//        }
//
//        for (int i = 0; i < searchResponse.getResponses().length; i++) {
//            MultiSearchResponse.Item item = searchResponse.getResponses()[i];
//            if (item.isFailure()) {
//                return new ArrayList<>();
//            }
//            List<Document> documentList = new ArrayList<>();
//            for (SearchHit hit : item.getResponse().getHits()) {
//                Document document = new Document(hit.getId(), hit.getSourceAsMap(),
//                        hit.getFields().get("_routing") == null ? null : hit.getFields().get("_routing").getValue());
//                documentList.add(document);
//            }
//            result.add(documentList);
//        }
//
//        return result;
//    }
//
//    @Data
//    @AllArgsConstructor
//    public static class ParentIdAndRouting {
//        private String parentId;
//        private String routing;
//    }
//
//
//    private List<QueryBuilder> builderFilter(List filters) {
//        List<QueryBuilder> queryBuilders = new ArrayList<>();
//        if (filters.isEmpty()) {
//            return queryBuilders;
//        }
//        filters.forEach(filter -> {
//            if (filter instanceof GeoFilter) {
//                GeoFilter geoFilter = (GeoFilter) filter;
//                GeoDistanceQueryBuilder geoDistanceQueryBuilder = new GeoDistanceQueryBuilder(geoFilter.fieldName);
//                geoDistanceQueryBuilder.distance(geoFilter.distance, DistanceUnit.METERS);
//                geoDistanceQueryBuilder.point(geoFilter.lat, ((GeoFilter) filter).lon);
//                queryBuilders.add(geoDistanceQueryBuilder);
//            }
//            if (filter instanceof DateRangeFilter) {
//                DateRangeFilter dateRangeFilter = (DateRangeFilter) filter;
//                RangeQueryBuilder rangeQueryBuilder = new RangeQueryBuilder(dateRangeFilter.fieldName);
//                String format = dateRangeFilter.format;
//                if (null != format && !"".equals(format)) {
//                    rangeQueryBuilder.format(format);
//                }
//                rangeQueryBuilder.gte(dateRangeFilter.gte);
//                rangeQueryBuilder.lte(dateRangeFilter.lte);
//                queryBuilders.add(rangeQueryBuilder);
//            }
//            if (filter instanceof TermsRangeFilter) {
//                TermsRangeFilter termsRangeFilter = (TermsRangeFilter) filter;
//                RangeQueryBuilder rangeQueryBuilder = new RangeQueryBuilder(termsRangeFilter.fieldName);
//                rangeQueryBuilder.gte(termsRangeFilter.gte);
//                rangeQueryBuilder.lte(termsRangeFilter.lte);
//                queryBuilders.add(rangeQueryBuilder);
//
//            }
//            if (filter instanceof NumbersRangeFilter) {
//                NumbersRangeFilter numbersRangeFilter = (NumbersRangeFilter) filter;
//                RangeQueryBuilder rangeQueryBuilder = new RangeQueryBuilder(numbersRangeFilter.fieldName);
//                rangeQueryBuilder.gte(numbersRangeFilter.gte);
//                rangeQueryBuilder.lte(numbersRangeFilter.lte);
//                queryBuilders.add(rangeQueryBuilder);
//            }
//
//        });
//        return queryBuilders;
//    }
//
//    /**
//     * 根据filters 生成QueryBuilder参数
//     *
//     * @return
//     */
//    private List<QueryBuilder> builderFilter() {
//        return builderFilter(this.filters);
//    }
//
//    /**
//     * 文档条件过滤
//     */
//    public static class Filter {
//        public Filter(String fieldName) {
//            this.fieldName = fieldName;
//        }
//
//        public String fieldName;
//    }
//
//    /**
//     * 距离过滤
//     * 使用米为默认单位
//     */
//    public static class GeoFilter extends Filter {
//        public double lat;
//        public double lon;
//        public double distance;
//
//        public GeoFilter(String fieldName, double lat, double lon, double distance) {
//            super(fieldName);
//            this.lat = lat;
//            this.lon = lon;
//            this.distance = distance;
//        }
//    }
//
//    /**
//     * 日期范围过滤
//     * 如不设置 format 将使用elasticsearch默认格式，支持的格式请参考：
//     * https://www.elastic.co/guide/en/elasticsearch/reference/6.4/mapping-date-format.html#built-in-date-formats
//     */
//    public static class DateRangeFilter extends Filter {
//        public String gte;
//        public String lte;
//        public String format;
//
//        public DateRangeFilter(String fieldName, String gte, String lte) {
//            super(fieldName);
//            this.gte = gte;
//            this.lte = lte;
//        }
//    }
//
//    /**
//     * TERM范围过虑
//     */
//    public static class TermsRangeFilter extends Filter {
//        public String gte;
//        public String lte;
//
//        public TermsRangeFilter(String fieldName, String gte, String lte) {
//            super(fieldName);
//            this.gte = gte;
//            this.lte = lte;
//        }
//    }
//
//    /**
//     * 数学范围过滤
//     */
//    public static class NumbersRangeFilter extends Filter {
//        public double gte;
//        public double lte;
//
//        public NumbersRangeFilter(String fieldName, double gte, double lte) {
//            super(fieldName);
//            this.gte = gte;
//            this.lte = lte;
//        }
//    }
//
//    @Data
//    @AllArgsConstructor
//    public static class SearchResponseObject {
//        private List<Map<String, Object>> resultList;
//        private long total;
//    }
//
//    @Data
//    @AllArgsConstructor
//    @NoArgsConstructor
//    public static class MultiParamObject {
//        /**
//         * 必须match的键值对
//         */
//        private Map<String, List<Object>> mustMatchMap;
//        /**
//         * 必须匹配的键值对
//         */
//        private Map<String, List<Object>> mustMap;
//        /**
//         * 应该匹配的键值对,or关系
//         */
//        private Map<String, List<Object>> orMap;
//        /**
//         * 必须不匹配的键值对
//         */
//        private Map<String, List<Object>> mustNotMap;
//        /**
//         * 查询没有此字段的数据
//         */
//        private List<String> missingFieldList;
//        /**
//         * 查询有此字段的数据
//         */
//        private List<String> mustExistFieldList;
//        /**
//         * 范围查询，key:字段; value: key:大于小于符号(gt,lt,gte,lte)，value:int数字
//         */
//        private Map<String, Map<String, Integer>> rangeFieldMap;
//        /**
//         * 范围查询，key:字段; value: key:大于小于符号(gt,lt,gte,lte)，value:数字
//         */
//        private Map<String, Map<String, Object>> rangeFieldObjectMap;
//
//        /**
//         * 模糊查询 key: 字段名称 value集合
//         */
//        private Map<String, String> fuzzyQuery;
//        /**
//         * 起始位置，从0开始
//         */
//        private int offset = 0;
//        /**
//         * 取多少个
//         */
//        private int limit = 1;
//        /**
//         * 路由
//         */
//        private String routing;
//        /**
//         * 过滤
//         */
//        private List filters;
//        /**
//         * 排序字段
//         */
//        private String sortField;
//        /**
//         * 排序方向，升序：asc 降序：desc
//         */
//        private String sortOrder;
//        /**
//         * 设置指定分片，（设置为用户的登录名可以保证返回结果一致）
//         */
//        private String preference;
//    }
//
//}
