package com.mx.server.elasticsearch.pool;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author limk
 */
@Getter
@Setter
@ToString
@Component
public class HighLevelClientConfig extends GenericObjectPoolConfig {

    @Autowired
    private HighLevelClientProperties properties;

    private List<String> hosts;
    private List<Integer> ports;
    private String username;
    private String password;

    public HighLevelClientConfig() {

        List<String> hosts = new ArrayList<>();
        List<Integer> ports = new ArrayList<>();
        String[] nodes = properties.getNodes().split(",");
        for (String node : nodes) {
            String[] hostAndPort = node.split(":");
            hosts.add(hostAndPort[0]);
            ports.add(Integer.parseInt(hostAndPort[1]));
        }

        this.hosts = hosts;
        this.ports = ports;
        this.username = properties.getUsername();
        this.password = properties.getPassword();
    }
}
