package com.prafka.core.connection;

/**
 * Enumeration of supported authentication methods for connecting to Kafka ecosystem components.
 *
 * <p>This enum defines the various authentication mechanisms that can be used when establishing
 * connections to Kafka brokers, Schema Registry, and Kafka Connect.
 */
public enum AuthenticationMethod {

    /**
     * No authentication required. Used for unsecured connections.
     */
    NONE,

    /**
     * HTTP Basic authentication using username and password.
     */
    BASIC,

    /**
     * Token-based authentication, typically using bearer tokens (OAuth2).
     */
    TOKEN,

    /**
     * SASL (Simple Authentication and Security Layer) authentication for Kafka brokers.
     */
    SASL,

    /**
     * SSL/TLS certificate-based authentication.
     */
    SSL,
}
