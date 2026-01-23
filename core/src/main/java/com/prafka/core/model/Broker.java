package com.prafka.core.model;

import lombok.Getter;

/**
 * Represents a Kafka broker in the cluster.
 *
 * <p>Extends {@link Node} with additional broker-specific information,
 * including whether this broker is currently the cluster controller.
 *
 * @see Node
 */
@Getter
public class Broker extends Node {

    private final boolean controller;

    public Broker(org.apache.kafka.common.Node source, boolean controller) {
        super(source);
        this.controller = controller;
    }
}
