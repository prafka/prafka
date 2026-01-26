package com.prafka.desktop.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class ValidateUtilsTest {

    @Test
    void shouldGetAdditionalProperties() {
        var map = new LinkedHashMap<String, String>() {{
            put("key1", "value1");
            put("key2", "value2");
        }};
        var result = ValidateUtils.getAdditionalProperties(map);

        assertEquals("key1=value1\nkey2=value2", result);
    }

    @Test
    void shouldGetAdditionalPropertiesReturnNullForEmptyMap() {
        // Given
        var map = new LinkedHashMap<String, String>();

        // When
        var result = ValidateUtils.getAdditionalProperties(map);

        // Then
        assertNull(result);
    }

    @Test
    void shouldGetAdditionalPropertiesReturnNullForNullMap() {
        // When
        var result = ValidateUtils.getAdditionalProperties((LinkedHashMap<String, String>) null);

        // Then
        assertNull(result);
    }

    @Test
    void shouldIsUrl() {
        assertTrue(ValidateUtils.isUrl("http://www.example.com"));
        assertTrue(ValidateUtils.isUrl("https://www.example.com"));
        assertTrue(ValidateUtils.isNotUrl("wrong"));
    }

    @Test
    void shouldIsUrlReturnFalseForNull() {
        // When
        var result = ValidateUtils.isUrl(null);

        // Then
        assertFalse(result);
    }

    @Test
    void shouldIsUrlReturnFalseForBlank() {
        // When
        var result = ValidateUtils.isUrl("");

        // Then
        assertFalse(result);
    }

    @Test
    void shouldIsUrlReturnFalseForWhitespace() {
        // When
        var result = ValidateUtils.isUrl("   ");

        // Then
        assertFalse(result);
    }

    @Test
    void shouldIsNotUrlReturnTrueForNonUrl() {
        // When
        var result = ValidateUtils.isNotUrl("just-text");

        // Then
        assertTrue(result);
    }

    @Test
    void shouldIsNotUrlReturnTrueForNull() {
        // When
        var result = ValidateUtils.isNotUrl(null);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldIsNotUrlReturnFalseForValidUrl() {
        // When
        var result = ValidateUtils.isNotUrl("https://example.com");

        // Then
        assertFalse(result);
    }

    @Test
    void shouldIsUrlHandleHttpWithPort() {
        // When
        var result = ValidateUtils.isUrl("http://localhost:8080");

        // Then
        assertTrue(result);
    }

    @Test
    void shouldIsUrlHandleHttpsWithPath() {
        // When
        var result = ValidateUtils.isUrl("https://example.com/path/to/resource");

        // Then
        assertTrue(result);
    }
}