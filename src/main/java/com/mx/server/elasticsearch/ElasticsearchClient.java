package com.mx.server.elasticsearch;

import com.mx.server.elasticsearch.document.DocumentUtility;
import com.mx.server.elasticsearch.index.IndexUtility;
import com.mx.server.elasticsearch.search.SearchUtility;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by lnkdwjl on 18-11-8.
 */
public class ElasticsearchClient {
    private RestHighLevelClient client;

    public ElasticsearchClient(HttpHost... host) {
        /**
         * 很多批量请求,重新设定超时时间
         * */
        RestClientBuilder builder = RestClient.builder(host)
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(-1)
                        .setSocketTimeout(-1))
                .setMaxRetryTimeoutMillis(200000);
        this.client = new RestHighLevelClient(builder);

    }

    public ElasticsearchClient(UsernamePasswordCredentials usernamePasswordCredentials, HttpHost... host) {
        /**
         * 很多批量请求,重新设定超时时间
         * */
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, usernamePasswordCredentials);
        RestClientBuilder builder = RestClient.builder(host)
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(-1)
                        .setSocketTimeout(-1))
                .setMaxRetryTimeoutMillis(200000);
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            httpClientBuilder.disableAuthCaching();
            return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        });
        this.client = new RestHighLevelClient(builder);
    }

    /**
     * 文档操作类
     */
    public DocumentUtility getDocumentUtility() {
        return new DocumentUtility(client);
    }

    /**
     * 索引操作类
     */
    public IndexUtility getIndexUtility() {
        return new IndexUtility(client);
    }

    /**
     * 检索操作类
     */
    public SearchUtility getSearchUtility() {
        return new SearchUtility(client);
    }

    public void close() {
        try {
            this.client.close();
        } catch (Exception e) {
            Logger.getLogger(ElasticsearchClient.class.getName()).log(Level.WARNING, e.getMessage());
        } finally {
            try {
                this.client.close();
            } catch (Exception e) {
                Logger.getLogger(ElasticsearchClient.class.getName()).log(Level.WARNING, e.getMessage());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ElasticsearchClient elasticsearchClient = new ElasticsearchClient(new HttpHost("localhost", 9200, "http"));
        SearchUtility searchUtility = elasticsearchClient.getSearchUtility();
        IndexUtility indexUtility = elasticsearchClient.getIndexUtility();
        DocumentUtility documentUtility = elasticsearchClient.getDocumentUtility();
        Map<String, List<Object>> keyValueMap = new HashMap<>();
        keyValueMap.put("address", new ArrayList(){{add("mill");}});
        SearchUtility.SearchResponseObject responseObject = searchUtility.terms("bank", "_doc", keyValueMap, 0, 5, "");
        System.out.println(responseObject.getTotal());
    }
}
