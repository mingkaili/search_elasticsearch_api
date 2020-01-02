package com.mx.server.elasticsearch.request;

import com.mx.server.elasticsearch.request.args.DeleteRequestArgs;
import com.mx.server.elasticsearch.request.args.SearchRequestTermLevelArgs;
import com.mx.server.elasticsearch.request.args.UpdateRequestArgs;
import java.util.Map;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.util.StringUtils;

/**
 * @author limk
 */
public class Request {

    private final static String SOURCE_TYPE = "_doc";

    /**
     * 插入请求组装
     * @param index 索引
     * @param id documentId
     * @param json document内容（json文本）
     * @return IndexRequest
     */
    public IndexRequest buildIndexRequest(String index, String id, String json) {
        IndexRequest request = new IndexRequest(index, SOURCE_TYPE, id);
        request.source(json, XContentType.JSON);

        return request;
    }

    /**
     * 插入请求组装
     *
     * @param updateRequestArgs 请求参数对象
     * @return UpdateRequest
     */
    public UpdateRequest buildUpdateRequest(UpdateRequestArgs updateRequestArgs) {
        UpdateRequest request = new UpdateRequest(updateRequestArgs.getIndex(), ElasticSearchConstants.SOURCE_TYPE,
            updateRequestArgs.getId());
        request.doc(updateRequestArgs.getJsonData(), XContentType.JSON);
        if (!StringUtils.isEmpty(updateRequestArgs.getRouting())) {
            request.routing(updateRequestArgs.getRouting());
        }
        request.retryOnConflict(3);

        return request;
    }

    /**
     * 删除请求组装
     *
     * @param deleteRequestArgs 请求参数
     * @return DeleteRequest
     */
    public DeleteRequest buildDeleteRequest(DeleteRequestArgs deleteRequestArgs) {
        DeleteRequest deleteRequest = new DeleteRequest(deleteRequestArgs.getIndex(),
            ElasticSearchConstants.SOURCE_TYPE, deleteRequestArgs.getId());
        if (StringUtils.isEmpty(deleteRequestArgs.getRouting())) {
            deleteRequest.routing(deleteRequestArgs.getRouting());
        }
        return deleteRequest;
    }

    /**
     * term级检索请求组装（现在是最low）
     *
     * @param searchRequestTermLevelArgs 请求参数
     * @return SearchRequest
     */
    public SearchRequest buildSearchRequest(SearchRequestTermLevelArgs searchRequestTermLevelArgs) {

        SearchRequest searchRequest = new SearchRequest(searchRequestTermLevelArgs.getIndex());

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders
            .termQuery(searchRequestTermLevelArgs.getTermKey() + "", searchRequestTermLevelArgs.getTermValue()));

        searchRequest.source(sourceBuilder);

        return searchRequest;
    }

}
