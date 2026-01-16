package com.prafka.desktop.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProxyModel {

    private Type type = Type.NO;
    private String host;
    private Integer port;
    private String user;
    private String password;

    public enum Type {
        NO,
        MANUAL,
    }
}
