package com.mx.server.elasticsearch.request.args;

/**
 * @author limk
 */
public class SearchRequestTermLevelArgs {

    private String index;
    private String termKey;
    private String termValue;

    public SearchRequestTermLevelArgs(String index, String termKey, String termValue) {
        this.index = index;
        this.termKey = termKey;
        this.termValue = termValue;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getTermKey() {
        return termKey;
    }

    public void setTermKey(String termKey) {
        this.termKey = termKey;
    }

    public String getTermValue() {
        return termValue;
    }

    public void setTermValue(String termValue) {
        this.termValue = termValue;
    }
}
