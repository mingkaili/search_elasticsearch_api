package com.mx.server.elasticsearch.request.args;

/**
 * @author limk
 */
public class IndexRequestArgs {

    private String index;
    private String id;
    private String jsonData;

    public IndexRequestArgs(String index, String id, String jsonData) {
        this.index = index;
        this.id = id;
        this.jsonData = jsonData;
    }
}
