package com.mx.server.elasticsearch.request.args;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author limk
 */
@Getter
@Setter
@ToString
public class DeleteRequestArgs {

    private String index;
    private String id;

    public DeleteRequestArgs(String index, String id) {
        this.index = index;
        this.id = id;
    }

}
