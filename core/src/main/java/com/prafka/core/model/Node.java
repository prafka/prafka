package com.prafka.core.model;

import lombok.Getter;

/**
 * Represents a node in the Kafka cluster.
 *
 * <p>Contains basic node information including the node ID, host, port, and optional rack.
 * This is the base class for more specific node types like {@link Broker}.
 *
 * @see Broker
 * @see org.apache.kafka.common.Node
 */
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
