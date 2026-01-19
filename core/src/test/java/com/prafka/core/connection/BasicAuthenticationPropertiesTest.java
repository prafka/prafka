package com.prafka.core.connection;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BasicAuthenticationPropertiesTest {

    @Test
    void shouldFillProperties() {
        // Given
        var expectedUsername = "testUser";
        var expectedPassword = "testPass";

        var basicAuthenticationProperties = BasicAuthenticationProperties.builder()
                .username(expectedUsername)
                .password(expectedPassword.toCharArray())
                .build();

        // When
        var properties = basicAuthenticationProperties.properties((username, password) ->
                Map.of("username", username, "password", new String(password)));

        // Then
        assertEquals(expectedUsername, properties.getProperty("username"));
        assertEquals(expectedPassword, properties.getProperty("password"));
    }
}