package com.mx.server.elasticsearch.pool;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

/**
 * @author limk
 * @date 2020/01/02
 */
public class ElasticsearchPoolFactory implements PooledObjectFactory<RestHighLevelClient> {

    private final static String SCHEME = "http";

    private HighLevelClientConfig clientConfig;

    public ElasticsearchPoolFactory(HighLevelClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    @Override
    public PooledObject<RestHighLevelClient> makeObject() throws Exception {

        HttpHost[] httpHosts = new HttpHost[clientConfig.getHosts().size()];
        for (int i = 0; i < clientConfig.getHosts().size(); i++) {
            httpHosts[i] = new HttpHost(clientConfig.getHosts().get(i), clientConfig.getPorts().get(i), SCHEME);
        }
        RestClientBuilder clientBuilder = RestClient.builder(httpHosts);

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
            new UsernamePasswordCredentials(clientConfig.getUsername(), clientConfig.getPassword()));

        clientBuilder.setHttpClientConfigCallback(httpClientBuilder -> {
            httpClientBuilder.disableAuthCaching();
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            return httpClientBuilder;
        });

        RestHighLevelClient client = new RestHighLevelClient(clientBuilder);

        return new DefaultPooledObject<>(client);
    }

    @Override
    public void destroyObject(PooledObject<RestHighLevelClient> p) throws Exception {
        RestHighLevelClient client = p.getObject();
        if (client != null && client.ping()) {
            client.close();
        }
    }

    @Override
    public boolean validateObject(PooledObject<RestHighLevelClient> p) {
        RestHighLevelClient client = p.getObject();
        try {
            return client.ping();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void activateObject(PooledObject<RestHighLevelClient> p) throws Exception {

    }

    @Override
    public void passivateObject(PooledObject<RestHighLevelClient> p) throws Exception {

    }

}
