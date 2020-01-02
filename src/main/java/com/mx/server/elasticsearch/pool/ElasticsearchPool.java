package com.mx.server.elasticsearch.pool;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.stereotype.Component;

/**
 * @author limk
 * @Date 2020/01/02
 */
@Component
public class ElasticsearchPool extends GenericObjectPool<RestHighLevelClient> {

    public ElasticsearchPool(HighLevelClientConfig config) {
        super(new ElasticsearchPoolFactory(config), config);
    }

}
