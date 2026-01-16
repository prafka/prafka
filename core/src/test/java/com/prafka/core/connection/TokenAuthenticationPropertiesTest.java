package com.prafka.core.connection;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TokenAuthenticationPropertiesTest {

    @Test
    void shouldFillProperties() {
        // Given
        var expectedToken = "testToken";

        var tokenAuthenticationProperties = TokenAuthenticationProperties.builder()
                .token(expectedToken)
                .build();

        // When
        var properties = tokenAuthenticationProperties.properties(token -> Map.of("token", token));

        // Then
        assertEquals(expectedToken, properties.getProperty("token"));
    }
}