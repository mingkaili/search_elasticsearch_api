package com.mx.server.elasticsearch.pool;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author limk
 * @date 2020/01/02
 */
@Getter
@Setter
@ToString
@Component
public class HighLevelClientProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(HighLevelClientProperties.class);

    @Value("${elasticsearch.nodes}")
    private String nodes;

    @Value("${elasticsearch.username}")
    private String username;

    @Value("${elasticsearch.password}")
    private String password;
}
