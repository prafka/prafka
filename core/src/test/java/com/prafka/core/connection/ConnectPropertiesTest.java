package com.prafka.core.connection;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConnectPropertiesTest {

    @Test
    void shouldFillUrlProperty() {
        // Given
        var expectedUrl = "jdbc:test://localhost:5432/testdb";

        var connectProperties = ConnectProperties.builder()
                .url(expectedUrl)
                .build();

        // When
        var properties = connectProperties.properties();

        // Then
        assertEquals(expectedUrl, properties.getProperty("url"));
    }

    @Test
    void shouldFillBasicAuthenticationProperties() {
        // Given
        var expectedUsername = "testUser";
        var expectedPassword = "testPassword";

        var basicAuthenticationProperties = BasicAuthenticationProperties.builder()
                .username(expectedUsername)
                .password(expectedPassword.toCharArray());

        var connectProperties = ConnectProperties.builder()
                .url("")
                .authenticationMethod(AuthenticationMethod.BASIC)
                .basicAuthenticationProperties(basicAuthenticationProperties)
                .build();

        // When
        var properties = connectProperties.properties();

        // Then
        assertEquals(expectedUsername, properties.getProperty("basic.username"));
        assertEquals(expectedPassword, properties.getProperty("basic.password"));
    }

    @Test
    void shouldFillSslAuthenticationProperties() {
        // Given
        var sslAuthenticationProperties = SslAuthenticationProperties.builder();

        var connectProperties = ConnectProperties.builder()
                .url("")
                .authenticationMethod(AuthenticationMethod.SSL)
                .sslAuthenticationProperties(sslAuthenticationProperties)
                .build();

        // When
        var properties = connectProperties.properties();

        // Then
        assertEquals(SecurityProtocol.SSL.name(), properties.getProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
    }

    @Test
    void shouldFillAdditionalProperties() {
        // Given
        var additionalProperties = Map.of("custom.property", "customValue");

        var connectProperties = ConnectProperties.builder()
                .url("")
                .additionalProperties(additionalProperties)
                .build();

        // When
        var properties = connectProperties.properties();

        // Then
        assertEquals("customValue", properties.getProperty("custom.property"));
    }
}