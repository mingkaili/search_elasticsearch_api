//package com.mx.server.elasticsearch;
//
//import com.mx.server.elasticsearch.pool.ElasticSearchPool;
//import com.mx.server.elasticsearch.pool.ElasticSearchPoolConfig;
//import com.mx.server.elasticsearch.search.SearchUtility;
//import org.apache.http.HttpHost;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * @program: elasticsearch-utility
// * @description:
// * @author: Murphy
// * @create: 2019-07-18 14:16
// **/
//public class ElasticSearchPoolTest {
//    public static void main(String[] args) throws Exception {
//        ElasticSearchPoolConfig config = new ElasticSearchPoolConfig();
//        config.setMaxTotal(100);
//        config.setHttpHost(new HttpHost("192.168.3.190", 9200, "http"));
//        ElasticSearchPool pool = new ElasticSearchPool(config);
//
//        Map<String, List<Object>> paramMap = new HashMap<>();
//        paramMap.put("documentid", new ArrayList() {{
//            add("89da4ba7-535e-415d-9595-5d60d9f7fc58");
//        }});
//
//        long start = System.currentTimeMillis();
//        for (int i = 0; i < 1000; i++) {
//            ElasticsearchClient client = pool.getResource();
//            SearchUtility searchUtility = client.getSearchUtility();
//
//            SearchUtility.SearchResponseObject responseObject = searchUtility.terms("sw19q1", "_doc", paramMap, 0, 1, "1157627904");
//
//            System.out.println(responseObject.getResultList().get(0).get("name") + " 第" + i + "个");
//            pool.returnResource(client);
//        }
//        long end = System.currentTimeMillis();
//        System.out.println("耗时(ms)：" + (end - start));
//
//    }
//}
