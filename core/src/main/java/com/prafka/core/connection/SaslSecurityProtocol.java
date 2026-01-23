package com.prafka.core.connection;

/**
 * Enumeration of SASL security protocols for Kafka connections.
 *
 * <p>Defines the transport layer security options when using SASL authentication.
 *
 * @see SaslAuthenticationProperties
 */
public enum SaslSecurityProtocol {

    /**
     * SASL authentication over plaintext connection. Authentication is secured via SASL,
     * but data is transmitted unencrypted.
     */
    SASL_PLAINTEXT,

    /**
     * SASL authentication over SSL/TLS encrypted connection. Both authentication and
     * data transmission are secured.
     */
    SASL_SSL,
}
