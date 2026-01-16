package com.prafka.core.connection;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SaslAuthenticationPropertiesTest {

    @Test
    void shouldFillPropertiesWithPlainSaslMechanism() {
        // Given
        var expectedUsername = "testUser";
        var expectedPassword = "testPassword";
        var saslAuthenticationProperties = SaslAuthenticationProperties.builder()
                .securityProtocol(SaslSecurityProtocol.SASL_PLAINTEXT)
                .mechanism(SaslMechanism.PLAIN)
                .username(expectedUsername)
                .password(expectedPassword)
                .build();

        // When
        var properties = saslAuthenticationProperties.properties();

        // Then
        assertEquals(SaslSecurityProtocol.SASL_PLAINTEXT.name(), properties.getProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
        assertEquals(SaslMechanism.PLAIN.getValue(), properties.getProperty(SaslConfigs.SASL_MECHANISM));
        assertTrue(properties.getProperty(SaslConfigs.SASL_JAAS_CONFIG).contains(expectedUsername));
        assertTrue(properties.getProperty(SaslConfigs.SASL_JAAS_CONFIG).contains(expectedPassword));
    }

    @Test
    void shouldFillPropertiesWithScramShaSaslMechanism() {
        // Given
        var expectedUsername = "testUser";
        var expectedPassword = "testPassword";
        var saslAuthenticationProperties = SaslAuthenticationProperties.builder()
                .securityProtocol(SaslSecurityProtocol.SASL_SSL)
                .mechanism(SaslMechanism.SCRAM_SHA_256)
                .username(expectedUsername)
                .password(expectedPassword)
                .build();

        // When
        var properties = saslAuthenticationProperties.properties();

        // Then
        assertEquals(SaslSecurityProtocol.SASL_SSL.name(), properties.getProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
        assertEquals(SaslMechanism.SCRAM_SHA_256.getValue(), properties.getProperty(SaslConfigs.SASL_MECHANISM));
        assertTrue(properties.getProperty(SaslConfigs.SASL_JAAS_CONFIG).contains(expectedUsername));
        assertTrue(properties.getProperty(SaslConfigs.SASL_JAAS_CONFIG).contains(expectedPassword));
    }
}