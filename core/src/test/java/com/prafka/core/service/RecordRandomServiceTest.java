package com.prafka.core.service;

import com.prafka.core.model.SerdeType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RecordRandomServiceTest {

    private final RecordRandomService service = new RecordRandomService();

    @Test
    void shouldGenerateRandomJson() {
        var result = service.random(SerdeType.JSON);

        assertNotNull(result);
        assertTrue(result.startsWith("{"));
        assertTrue(result.endsWith("}"));
    }

    @Test
    void shouldGenerateRandomShort() {
        var result = service.random(SerdeType.SHORT);

        assertNotNull(result);
        assertDoesNotThrow(() -> Short.parseShort(result));
    }

    @Test
    void shouldGenerateRandomInteger() {
        var result = service.random(SerdeType.INTEGER);

        assertNotNull(result);
        assertDoesNotThrow(() -> Integer.parseInt(result));
    }

    @Test
    void shouldGenerateRandomLong() {
        var result = service.random(SerdeType.LONG);

        assertNotNull(result);
        assertDoesNotThrow(() -> Long.parseLong(result));
    }

    @Test
    void shouldGenerateRandomFloat() {
        var result = service.random(SerdeType.FLOAT);

        assertNotNull(result);
        assertDoesNotThrow(() -> Float.parseFloat(result));
    }

    @Test
    void shouldGenerateRandomDouble() {
        var result = service.random(SerdeType.DOUBLE);

        assertNotNull(result);
        assertDoesNotThrow(() -> Double.parseDouble(result));
    }

    @Test
    void shouldGenerateRandomUuid() {
        var result = service.random(SerdeType.UUID);

        assertNotNull(result);
        assertDoesNotThrow(() -> UUID.fromString(result));
    }

    @Test
    void shouldGenerateRandomStringForDefaultCase() {
        var result = service.random(SerdeType.STRING);

        assertNotNull(result);
        assertTrue(result.length() >= 1);
        assertTrue(result.length() <= 512);
    }

    @Test
    void shouldGenerateRandomStringForNullSerde() {
        var result = service.random(SerdeType.NULL);

        assertNotNull(result);
        assertTrue(result.length() >= 1);
    }

    @Test
    void shouldGenerateRandomKey() {
        var result = service.randomKey(SerdeType.STRING);

        assertNotNull(result);
    }

    @Test
    void shouldGenerateRandomValue() {
        var result = service.randomValue(SerdeType.INTEGER);

        assertNotNull(result);
        assertDoesNotThrow(() -> Integer.parseInt(result));
    }

    @Test
    void shouldGenerateMultipleDifferentJsonValues() {
        var result1 = service.random(SerdeType.JSON);
        var result2 = service.random(SerdeType.JSON);

        assertNotNull(result1);
        assertNotNull(result2);
        assertTrue(result1.startsWith("{") && result1.endsWith("}"));
        assertTrue(result2.startsWith("{") && result2.endsWith("}"));
    }

    @Test
    void shouldGenerateDifferentRandomValues() {
        var results = new java.util.HashSet<String>();
        for (int i = 0; i < 10; i++) {
            results.add(service.random(SerdeType.UUID));
        }
        assertEquals(10, results.size());
    }

    @Test
    void shouldGenerateValidJsonStructure() {
        for (int i = 0; i < 5; i++) {
            var result = service.random(SerdeType.JSON);

            var openBraces = result.chars().filter(ch -> ch == '{').count();
            var closeBraces = result.chars().filter(ch -> ch == '}').count();
            assertEquals(openBraces, closeBraces, "JSON braces should be balanced");
        }
    }
}
