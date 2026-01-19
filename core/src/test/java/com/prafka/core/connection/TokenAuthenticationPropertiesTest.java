package com.prafka.core.connection;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TokenAuthenticationPropertiesTest {

    @Test
    void shouldFillProperties() {
        // Given
        var expectedToken = "testToken".toCharArray();

        var tokenAuthenticationProperties = TokenAuthenticationProperties.builder()
                .token(expectedToken)
                .build();

        // When
        var properties = tokenAuthenticationProperties.properties(token -> Map.of("token", new String(token)));

        // Then
        assertEquals(new String(expectedToken), properties.getProperty("token"));
    }
}