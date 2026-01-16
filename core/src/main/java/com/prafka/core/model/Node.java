package com.prafka.core.model;

import lombok.Getter;

@Getter
public class Node {

    private final int id;
    private final String host;
    private final int port;
    private final String rack;

    public Node(org.apache.kafka.common.Node source) {
        id = source.id();
        host = source.host();
        port = source.port();
        rack = source.rack();
    }

    public String getAddress() {
        return host + ":" + port;
    }
}
