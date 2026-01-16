package com.prafka.desktop.model;

public record AnalyticsModel(String version, String userId, Type type, Object payload) {
    public enum Type {
        FIRST_LAUNCH,
        GET_VIEW,
        UNCAUGHT_EXCEPTION,
    }
}
