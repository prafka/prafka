package com.prafka.desktop.model;

/**
 * Represents an analytics event to be sent to the backend server.
 *
 * <p>Captures application version, user identifier, event type, and associated payload data.
 */
public record AnalyticsModel(String version, String userId, Type type, Object payload) {
    public enum Type {
        FIRST_LAUNCH,
        GET_VIEW,
        UNCAUGHT_EXCEPTION,
    }
}
