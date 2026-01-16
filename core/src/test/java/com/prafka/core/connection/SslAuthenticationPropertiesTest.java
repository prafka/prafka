package com.prafka.core.connection;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SslAuthenticationPropertiesTest {

    @Test
    void shouldFillProperties() {
        // Given
        var expectedKeystoreLocation = "/path/to/keystore.jks";
        var expectedKeystorePassword = "keystorePass";
        var expectedKeyPassword = "keyPass";
        var expectedTruststoreLocation = "/path/to/truststore.jks";
        var expectedTruststorePassword = "truststorePass";

        var sslAuthenticationProperties = SslAuthenticationProperties.builder()
                .keystoreLocation(expectedKeystoreLocation)
                .keystorePassword(expectedKeystorePassword)
                .keyPassword(expectedKeyPassword)
                .truststoreLocation(expectedTruststoreLocation)
                .truststorePassword(expectedTruststorePassword)
                .build();

        // When
        var properties = sslAuthenticationProperties.properties();

        // Then
        assertEquals(SecurityProtocol.SSL.name(), properties.getProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
        assertEquals(expectedKeystoreLocation, properties.getProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG));
        assertEquals(expectedKeystorePassword, properties.getProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG));
        assertEquals(expectedKeyPassword, properties.getProperty(SslConfigs.SSL_KEY_PASSWORD_CONFIG));
        assertEquals(expectedTruststoreLocation, properties.getProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG));
        assertEquals(expectedTruststorePassword, properties.getProperty(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG));
    }
}
