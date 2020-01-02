package com.mx.server.elasticsearch.request.args;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author limk
 * @date 2019/07/26
 */
@Getter
@Setter
@ToString
public class GetRequestArgs {

    private String index;
    private String id;

    public GetRequestArgs(String index, String id) {
        this.index = index;
        this.id = id;
    }
}
