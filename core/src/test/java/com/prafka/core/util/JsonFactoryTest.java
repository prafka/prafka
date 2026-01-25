package com.prafka.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JsonFactoryTest {

    @Test
    void shouldSerializeMapToJson() {
        var map = Map.of("key", "value");

        var json = JsonFactory.gsonDefault.toJson(map);

        assertEquals("{\"key\":\"value\"}", json);
    }

    @Test
    void shouldDeserializeJsonToMapStringString() {
        var json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";

        var result = JsonFactory.gsonDefault.fromJson(json, JsonFactory.MAP_STING_STRING_TYPE);

        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
    }

    @Test
    void shouldDeserializeJsonToMapStringObject() {
        var json = "{\"string\":\"value\",\"number\":42,\"bool\":true}";

        var result = JsonFactory.gsonDefault.fromJson(json, JsonFactory.MAP_STING_OBJECT_TYPE);

        assertEquals(3, result.size());
        assertEquals("value", result.get("string"));
        assertEquals(42.0, result.get("number"));
        assertEquals(true, result.get("bool"));
    }

    @Test
    void shouldSerializeNullValues() {
        var map = new java.util.HashMap<String, String>();
        map.put("key", null);

        var json = JsonFactory.gsonDefault.toJson(map);

        assertEquals("{\"key\":null}", json);
    }

    @Test
    void shouldNotEscapeHtml() {
        var map = Map.of("key", "<html>&test</html>");

        var json = JsonFactory.gsonDefault.toJson(map);

        assertTrue(json.contains("<html>&test</html>"));
        assertFalse(json.contains("\\u003c"));
    }

    @Test
    void shouldPrettyPrintJson() {
        var map = Map.of("key", "value");

        var json = JsonFactory.gsonPretty.toJson(map);

        assertTrue(json.contains("\n"));
        assertTrue(json.contains("  "));
    }

    @Test
    void shouldSerializeOptionalPresent() {
        var optional = Optional.of("test-value");

        var json = JsonFactory.gsonDefault.toJson(optional);

        assertEquals("\"test-value\"", json);
    }

    @Test
    void shouldSerializeOptionalEmpty() {
        var optional = Optional.empty();

        var json = JsonFactory.gsonDefault.toJson(optional);

        assertEquals("null", json);
    }

    @Test
    void shouldSerializeOptionalNull() {
        Optional<String> optional = null;

        var json = JsonFactory.gsonDefault.toJson(optional);

        assertEquals("null", json);
    }

    @Test
    void shouldDeserializeOptionalPresent() {
        var json = "\"test-value\"";

        var result = JsonFactory.gsonDefault.fromJson(json, new com.google.gson.reflect.TypeToken<Optional<String>>() {}.getType());

        assertTrue(((Optional<?>) result).isPresent());
        assertEquals("test-value", ((Optional<?>) result).get());
    }

    @Test
    void shouldDeserializeOptionalNull() {
        var json = "null";

        var result = JsonFactory.gsonDefault.fromJson(json, new com.google.gson.reflect.TypeToken<Optional<String>>() {}.getType());

        assertTrue(((Optional<?>) result).isEmpty());
    }

    @Test
    void shouldSerializeNestedOptional() {
        record TestRecord(String name, Optional<Integer> age) {}
        var record = new TestRecord("John", Optional.of(30));

        var json = JsonFactory.gsonDefault.toJson(record);

        assertTrue(json.contains("\"name\":\"John\""));
        assertTrue(json.contains("\"age\":30"));
    }

    @Test
    void shouldUseObjectMapper() throws JsonProcessingException {
        var map = Map.of("key", "value");

        var json = JsonFactory.objectMapperDefault.writeValueAsString(map);

        assertEquals("{\"key\":\"value\"}", json);
    }

    @Test
    void shouldObjectMapperDeserialize() throws JsonProcessingException {
        var json = "{\"key\":\"value\"}";

        @SuppressWarnings("unchecked")
        var result = JsonFactory.objectMapperDefault.readValue(json, Map.class);

        assertEquals("value", result.get("key"));
    }
}
