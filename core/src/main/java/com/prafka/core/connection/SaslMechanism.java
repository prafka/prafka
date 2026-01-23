package com.prafka.core.connection;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumeration of supported SASL authentication mechanisms for Kafka connections.
 *
 * <p>SASL (Simple Authentication and Security Layer) provides a framework for authentication
 * in network protocols. This enum defines the SASL mechanisms supported for Kafka authentication.
 *
 * @see SaslAuthenticationProperties
 */
@Getter
@RequiredArgsConstructor
public enum SaslMechanism {

    /**
     * SCRAM-SHA-256 mechanism. Provides challenge-response authentication using SHA-256 hashing.
     */
    SCRAM_SHA_256("SCRAM-SHA-256"),

    /**
     * SCRAM-SHA-512 mechanism. Provides challenge-response authentication using SHA-512 hashing.
     */
    SCRAM_SHA_512("SCRAM-SHA-512"),

    /**
     * PLAIN mechanism. Transmits credentials in plain text (should only be used with SSL/TLS).
     */
    PLAIN("PLAIN");

    private final String value;
}
