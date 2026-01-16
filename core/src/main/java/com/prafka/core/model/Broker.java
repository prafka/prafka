package com.prafka.core.model;

import lombok.Getter;

@Getter
public class Broker extends Node {

    private final boolean controller;

    public Broker(org.apache.kafka.common.Node source, boolean controller) {
        super(source);
        this.controller = controller;
    }
}
