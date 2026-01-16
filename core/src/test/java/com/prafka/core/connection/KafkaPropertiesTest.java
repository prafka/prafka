package com.prafka.core.connection;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KafkaPropertiesTest {

    @Test
    void shouldFillBootstrapServersProperty() {
        // Given
        var expectedBootstrapServers = "localhost:9092";

        var kafkaProperties = KafkaProperties.builder()
                .bootstrapServers(expectedBootstrapServers)
                .build();

        // When
        var properties = kafkaProperties.properties();

        // Then
        assertEquals(expectedBootstrapServers, properties.getProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG));
    }


    @Test
    void shouldFillSaslAuthenticationProperties() {
        // Given
        var saslAuthenticationProperties = SaslAuthenticationProperties.builder()
                .securityProtocol(SaslSecurityProtocol.SASL_PLAINTEXT)
                .mechanism(SaslMechanism.PLAIN);

        var kafkaProperties = KafkaProperties.builder()
                .bootstrapServers("")
                .authenticationMethod(AuthenticationMethod.SASL)
                .saslAuthenticationProperties(saslAuthenticationProperties)
                .build();

        // When
        var properties = kafkaProperties.properties();

        // Then
        assertEquals(SaslSecurityProtocol.SASL_PLAINTEXT.name(), properties.getProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
        assertEquals(SaslMechanism.PLAIN.getValue(), properties.getProperty(SaslConfigs.SASL_MECHANISM));
    }

    @Test
    void shouldFillSslAuthenticationProperties() {
        // Given
        var kafkaProperties = KafkaProperties.builder()
                .bootstrapServers("")
                .authenticationMethod(AuthenticationMethod.SSL)
                .sslAuthenticationProperties(SslAuthenticationProperties.builder())
                .build();

        // When
        var properties = kafkaProperties.properties();

        // Then
        assertEquals(SecurityProtocol.SSL.name(), properties.getProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
    }

    @Test
    void shouldFillAdditionalProperties() {
        // Given
        var additionalProperties = Map.of("custom.property", "customValue");

        var kafkaProperties = KafkaProperties.builder()
                .bootstrapServers("")
                .additionalProperties(additionalProperties)
                .build();

        // When
        var properties = kafkaProperties.properties();

        // Then
        assertEquals("customValue", properties.getProperty("custom.property"));
    }
}