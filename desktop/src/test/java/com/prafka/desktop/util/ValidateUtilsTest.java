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
    void shouldIsUrl() {
        assertTrue(ValidateUtils.isUrl("http://www.example.com"));
        assertTrue(ValidateUtils.isUrl("https://www.example.com"));
        assertTrue(ValidateUtils.isNotUrl("wrong"));
    }
}