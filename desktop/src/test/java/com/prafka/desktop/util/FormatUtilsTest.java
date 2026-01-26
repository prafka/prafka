package com.prafka.desktop.util;

import com.prafka.core.model.SerdeType;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormatUtilsTest {

    @Test
    void shouldPrettyDurationInMs() {
        assertEquals("500ms", FormatUtils.prettyDurationInMs(500L));
        assertEquals("5s", FormatUtils.prettyDurationInMs(5000L));
        assertEquals("5s 500ms", FormatUtils.prettyDurationInMs(5500L));
        assertEquals("5min", FormatUtils.prettyDurationInMs(300000L));
        assertEquals("5min 30s", FormatUtils.prettyDurationInMs(330000L));
        assertEquals("2h", FormatUtils.prettyDurationInMs(7200000L));
        assertEquals("2h 30min", FormatUtils.prettyDurationInMs(9000000L));
        assertEquals("3d", FormatUtils.prettyDurationInMs(259200000L));
        assertEquals("2d 3h 4min 5s 600ms", FormatUtils.prettyDurationInMs(183845600L));
    }

    @Test
    void shouldPrettyDurationInMsHandleZero() {
        // When
        var result = FormatUtils.prettyDurationInMs(0L);

        // Then
        assertEquals("", result);
    }

    @Test
    void shouldPrettyDurationInMsHandleSmallValues() {
        // When
        var result = FormatUtils.prettyDurationInMs(1L);

        // Then
        assertEquals("1ms", result);
    }

    @Test
    void shouldPrettyDurationInMsFromString() {
        // When
        var result = FormatUtils.prettyDurationInMs("5000");

        // Then
        assertEquals("5s", result);
    }

    @Test
    void shouldPrettyEnum() {
        assertEquals("Schema registry", FormatUtils.prettyEnum(SerdeType.SCHEMA_REGISTRY));
    }

    @Test
    void shouldPrettyEnumHandleSingleWord() {
        // Given
        var singleWordEnum = SerdeType.STRING;

        // When
        var result = FormatUtils.prettyEnum(singleWordEnum);

        // Then
        assertEquals("String", result);
    }

    @Test
    void shouldPrettyEnumFromString() {
        // When
        var result = FormatUtils.prettyEnum("SOME_ENUM_VALUE");

        // Then
        assertEquals("Some enum value", result);
    }

    @Test
    void shouldPrettyEnumHandleMultipleUnderscores() {
        // When
        var result = FormatUtils.prettyEnum("A_B_C_D");

        // Then
        assertEquals("A b c d", result);
    }

    @Test
    void shouldSplitAclPrincipal() {
        assertEquals(Pair.of("user", "user"), FormatUtils.splitAclPrincipal("user:user"));
    }

    @Test
    void shouldSplitAclPrincipalHandleNoSeparator() {
        // When
        var result = FormatUtils.splitAclPrincipal("noSeparator");

        // Then
        assertEquals(Pair.of("noSeparator", null), result);
    }

    @Test
    void shouldSplitAclPrincipalHandleEmptyValue() {
        // When
        var result = FormatUtils.splitAclPrincipal("group:");

        // Then
        assertEquals(Pair.of("group", ""), result);
    }

    @Test
    void shouldPrettySizeInBytes() {
        // When
        var result = FormatUtils.prettySizeInBytes(1024L);

        // Then
        assertEquals("1 KB", result);
    }

    @Test
    void shouldPrettySizeInBytesFromString() {
        // When
        var result = FormatUtils.prettySizeInBytes("1048576");

        // Then
        assertEquals("1 MB", result);
    }

    @Test
    void shouldPrettyJsonFormatValidJson() {
        // Given
        var json = "{\"key\":\"value\"}";

        // When
        var result = FormatUtils.prettyJson(json);

        // Then
        assertTrue(result.contains("\"key\""));
        assertTrue(result.contains("\"value\""));
    }

    @Test
    void shouldPrettyJsonReturnOriginalForInvalidJson() {
        // Given
        var invalidJson = "not json";

        // When
        var result = FormatUtils.prettyJson(invalidJson);

        // Then
        assertEquals(invalidJson, result);
    }

    @Test
    void shouldPrettyAvroFormatAsJson() {
        // Given
        var avro = "{\"type\":\"record\"}";

        // When
        var result = FormatUtils.prettyAvro(avro);

        // Then
        assertTrue(result.contains("\"type\""));
    }

    @Test
    void shouldNAConstantBeDefined() {
        // Then
        assertEquals("N/A", FormatUtils.NA);
    }
}