package com.prafka.core.connection;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SchemaRegistryPropertiesTest {

    @Test
    void shouldFillUrlProperty() {
        // Given
        var expectedUrl = "jdbc:test://localhost:5432/testdb";

        var schemaRegistryProperties = SchemaRegistryProperties.builder()
                .url(expectedUrl)
                .build();

        // When
        var properties = schemaRegistryProperties.properties();

        // Then
        assertEquals(expectedUrl, properties.getProperty(SchemaRegistryClientConfig.CLIENT_NAMESPACE + "url"));
    }

    @Test
    void shouldFillBasicAuthenticationProperties() {
        // Given
        var expectedUsername = "testUser";
        var expectedPassword = "testPass";

        var basicAuthenticationProperties = BasicAuthenticationProperties.builder()
                .username(expectedUsername)
                .password(expectedPassword.toCharArray());

        var schemaRegistryProperties = SchemaRegistryProperties.builder()
                .url("")
                .authenticationMethod(AuthenticationMethod.BASIC)
                .basicAuthenticationProperties(basicAuthenticationProperties)
                .build();

        // When
        var properties = schemaRegistryProperties.properties();

        // Then
        assertEquals("USER_INFO", properties.getProperty(SchemaRegistryClientConfig.CLIENT_NAMESPACE + SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE));
        assertEquals(expectedUsername + ":" + expectedPassword, properties.getProperty(SchemaRegistryClientConfig.CLIENT_NAMESPACE + SchemaRegistryClientConfig.USER_INFO_CONFIG));
    }

    @Test
    void shouldFillTokenAuthenticationProperties() {
        // Given
        var expectedToken = "testToken";

        var tokenAuthenticationProperties = TokenAuthenticationProperties.builder()
                .token(expectedToken);

        var schemaRegistryProperties = SchemaRegistryProperties.builder()
                .url("")
                .authenticationMethod(AuthenticationMethod.TOKEN)
                .tokenAuthenticationProperties(tokenAuthenticationProperties)
                .build();

        // When
        var properties = schemaRegistryProperties.properties();

        // Then
        assertEquals("STATIC_TOKEN", properties.getProperty(SchemaRegistryClientConfig.CLIENT_NAMESPACE + SchemaRegistryClientConfig.BEARER_AUTH_CREDENTIALS_SOURCE));
        assertEquals(expectedToken, properties.getProperty(SchemaRegistryClientConfig.CLIENT_NAMESPACE + SchemaRegistryClientConfig.BEARER_AUTH_TOKEN_CONFIG));
    }

    @Test
    void shouldFillSslAuthenticationProperties() {
        // Given
        var sslAuthenticationProperties = SslAuthenticationProperties.builder();

        var schemaRegistryProperties = SchemaRegistryProperties.builder()
                .url("")
                .authenticationMethod(AuthenticationMethod.SSL)
                .sslAuthenticationProperties(sslAuthenticationProperties)
                .build();

        // When
        var properties = schemaRegistryProperties.properties();

        // Then
        assertEquals(SecurityProtocol.SSL.name(), properties.getProperty(SchemaRegistryClientConfig.CLIENT_NAMESPACE + CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
    }

    @Test
    void shouldFillAdditionalProperties() {
        // Given
        var additionalProperties = Map.of("custom.property", "customValue");

        var connectProperties = SchemaRegistryProperties.builder()
                .url("")
                .additionalProperties(additionalProperties)
                .build();

        // When
        var properties = connectProperties.properties();

        // Then
        assertEquals("customValue", properties.getProperty("custom.property"));
    }
}