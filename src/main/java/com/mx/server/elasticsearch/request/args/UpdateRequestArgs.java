package com.mx.server.elasticsearch.request.args;

/**
 * @author limk
 */
public class UpdateRequestArgs {

    private String index;
    private String id;
    private String jsonData;
    private String routing;

    public UpdateRequestArgs(String index, String id, String jsonData) {
        this.index = index;
        this.id = id;
        this.jsonData = jsonData;
    }

    public UpdateRequestArgs(String index, String id, String jsonData, String routing) {
        this.index = index;
        this.id = id;
        this.jsonData = jsonData;
        this.routing = routing;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJsonData() {
        return jsonData;
    }

    public void setJsonData(String jsonData) {
        this.jsonData = jsonData;
    }

    public String getRouting() {
        return routing;
    }

    public void setRouting(String routing) {
        this.routing = routing;
    }
}
