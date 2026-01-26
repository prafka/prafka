package com.prafka.desktop.util;

import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class CodeHighlightTest {

    @Test
    void shouldHighlightDefaultReturnSingleSpan() {
        // Given
        var text = "simple text";

        // When
        var result = CodeHighlight.highlightDefault(text);

        // Then
        assertNotNull(result);
        assertEquals(text.length(), result.length());
    }

    @Test
    void shouldHighlightDefaultContainDefaultStyleClass() {
        // Given
        var text = "simple text";

        // When
        var result = CodeHighlight.highlightDefault(text);

        // Then
        assertTrue(result.iterator().hasNext());
        assertTrue(result.iterator().next().getStyle().contains("default"));
    }

    @Test
    void shouldHighlightJsonIdentifyKeys() {
        // Given
        var json = "{\"name\": \"value\"}";

        // When
        var result = CodeHighlight.highlightJson(json);

        // Then
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    void shouldHighlightJsonIdentifyStrings() {
        // Given
        var json = "\"test-string\"";

        // When
        var result = CodeHighlight.highlightJson(json);

        // Then
        assertNotNull(result);
        assertTrue(containsStyleClass(result, "value-string"));
    }

    @Test
    void shouldHighlightJsonIdentifyNumbers() {
        // Given
        var json = "123";

        // When
        var result = CodeHighlight.highlightJson(json);

        // Then
        assertNotNull(result);
        assertTrue(containsStyleClass(result, "value-number"));
    }

    @Test
    void shouldHighlightJsonIdentifyBooleans() {
        // Given
        var json = "true";

        // When
        var result = CodeHighlight.highlightJson(json);

        // Then
        assertNotNull(result);
        assertTrue(containsStyleClass(result, "value-bool"));
    }

    @Test
    void shouldHighlightJsonIdentifyFalse() {
        // Given
        var json = "false";

        // When
        var result = CodeHighlight.highlightJson(json);

        // Then
        assertNotNull(result);
        assertTrue(containsStyleClass(result, "value-bool"));
    }

    @Test
    void shouldHighlightJsonIdentifyNull() {
        // Given
        var json = "null";

        // When
        var result = CodeHighlight.highlightJson(json);

        // Then
        assertNotNull(result);
        assertTrue(containsStyleClass(result, "value-string"));
    }

    @Test
    void shouldHighlightJsonNullCaseInsensitive() {
        // Given
        var json = "NULL";

        // When
        var result = CodeHighlight.highlightJson(json);

        // Then
        assertNotNull(result);
        assertTrue(containsStyleClass(result, "value-string"));
    }

    @Test
    void shouldHighlightJsonIdentifyFieldName() {
        // Given
        var json = "{\"fieldName\": 123}";

        // When
        var result = CodeHighlight.highlightJson(json);

        // Then
        assertNotNull(result);
        assertTrue(containsStyleClass(result, "field-name"));
    }

    @Test
    void shouldHighlightJsonHandleNegativeNumbers() {
        // Given
        var json = "-42";

        // When
        var result = CodeHighlight.highlightJson(json);

        // Then
        assertNotNull(result);
        assertTrue(containsStyleClass(result, "value-number"));
    }

    @Test
    void shouldHighlightJsonHandleDecimalNumbers() {
        // Given
        var json = "3.14";

        // When
        var result = CodeHighlight.highlightJson(json);

        // Then
        assertNotNull(result);
        assertTrue(containsStyleClass(result, "value-number"));
    }

    @Test
    void shouldHighlightKVIdentifyKeys() {
        // Given
        var text = "key=value";

        // When
        var result = CodeHighlight.highlightKV(text);

        // Then
        assertNotNull(result);
        assertTrue(containsStyleClass(result, "key"));
    }

    @Test
    void shouldHighlightKVIdentifyValues() {
        // Given
        var text = "key=value";

        // When
        var result = CodeHighlight.highlightKV(text);

        // Then
        assertNotNull(result);
        assertTrue(containsStyleClass(result, "value"));
    }

    @Test
    void shouldHighlightKVHandleMultipleLines() {
        // Given
        var text = "key1=value1\nkey2=value2";

        // When
        var result = CodeHighlight.highlightKV(text);

        // Then
        assertNotNull(result);
        assertEquals(text.length(), result.length());
    }

    @Test
    void shouldHighlightAvroDelegateToJson() {
        // Given
        var avro = "{\"type\": \"record\"}";

        // When
        var result = CodeHighlight.highlightAvro(avro);
        var jsonResult = CodeHighlight.highlightJson(avro);

        // Then
        assertEquals(jsonResult.length(), result.length());
    }

    @Test
    void shouldHighlightProtobufReturnDefault() {
        // Given
        var protobuf = "message Test {}";

        // When
        var result = CodeHighlight.highlightProtobuf(protobuf);

        // Then
        assertNotNull(result);
        assertEquals(protobuf.length(), result.length());
    }

    @Test
    void shouldHighlightJsReturnDefault() {
        // Given
        var js = "function test() {}";

        // When
        var result = CodeHighlight.highlightJs(js);

        // Then
        assertNotNull(result);
        assertEquals(js.length(), result.length());
    }

    @Test
    void shouldHighlightJsonHandleEmptyText() {
        // Given
        var json = "";

        // When
        var result = CodeHighlight.highlightJson(json);

        //Then
        assertNotNull(result);
    }

    @Test
    void shouldHighlightDefaultHandleEmptyText() {
        // Given
        var text = "";

        // When
        var result = CodeHighlight.highlightDefault(text);

        //Then
        assertNotNull(result);
    }

    @Test
    void shouldHighlightJsonHandleComplexObject() {
        // Given
        var json = "{\"name\": \"John\", \"age\": 30, \"active\": true, \"data\": null}";

        // When
        var result = CodeHighlight.highlightJson(json);

        // Then
        assertNotNull(result);
        assertTrue(containsStyleClass(result, "field-name"));
        assertTrue(containsStyleClass(result, "value-string"));
        assertTrue(containsStyleClass(result, "value-number"));
        assertTrue(containsStyleClass(result, "value-bool"));
    }

    private boolean containsStyleClass(org.fxmisc.richtext.model.StyleSpans<Collection<String>> spans, String styleClass) {
        for (var span : spans) {
            if (span.getStyle().contains(styleClass)) {
                return true;
            }
        }
        return false;
    }
}
