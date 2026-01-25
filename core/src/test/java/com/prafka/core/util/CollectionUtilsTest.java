package com.prafka.core.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class CollectionUtilsTest {

    @Test
    void shouldCreateMapOfFromKeyValuePairs() {
        var result = CollectionUtils.mapOf("key1", "value1", "key2", "value2");

        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
    }

    @Test
    void shouldCreateEmptyMap() {
        var result = CollectionUtils.mapOf();

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldCreateMapWithSingleEntry() {
        var result = CollectionUtils.mapOf("key", "value");

        assertEquals(1, result.size());
        assertEquals("value", result.get("key"));
    }

    @Test
    void shouldCreateMapWithMixedTypes() {
        var result = CollectionUtils.mapOf("stringKey", "stringValue", "intKey", 42, "boolKey", true);

        assertEquals(3, result.size());
        assertEquals("stringValue", result.get("stringKey"));
        assertEquals(42, result.get("intKey"));
        assertEquals(true, result.get("boolKey"));
    }

    @Test
    void shouldCreateMapWithNullValues() {
        var result = CollectionUtils.mapOf("key1", null, "key2", "value2");

        assertEquals(2, result.size());
        assertNull(result.get("key1"));
        assertEquals("value2", result.get("key2"));
    }

    @Test
    void shouldPreserveInsertionOrder() {
        var result = CollectionUtils.mapOf("c", 3, "a", 1, "b", 2);

        assertInstanceOf(LinkedHashMap.class, result);
        var keys = result.keySet().toArray();
        assertEquals("c", keys[0]);
        assertEquals("a", keys[1]);
        assertEquals("b", keys[2]);
    }
}
