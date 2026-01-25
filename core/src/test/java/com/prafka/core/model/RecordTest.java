package com.prafka.core.model;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecordTest {

    @Test
    void shouldDetectJsonObject() {
        var headers = new RecordHeaders();
        var source = new ConsumerRecord<>("topic", 0, 100L, 1234567890L,
                TimestampType.CREATE_TIME, 10, 20, "key".getBytes(), "{\"name\":\"value\"}".getBytes(), headers, null);

        var record = new Record(source, "key", "{\"name\":\"value\"}");

        assertTrue(record.isValueIsJson());
        assertEquals("{\"name\":\"value\"}", record.getValueCompressed());
    }

    @Test
    void shouldDetectJsonArray() {
        var headers = new RecordHeaders();
        var source = new ConsumerRecord<>("topic", 0, 100L, 1234567890L,
                TimestampType.CREATE_TIME, 10, 20, "key".getBytes(), "[1,2,3]".getBytes(), headers, null);

        var record = new Record(source, "key", "[1,2,3]");

        assertTrue(record.isValueIsJson());
    }

    @Test
    void shouldNotDetectPlainTextAsJson() {
        var headers = new RecordHeaders();
        var source = new ConsumerRecord<>("topic", 0, 100L, 1234567890L,
                TimestampType.CREATE_TIME, 10, 20, "key".getBytes(), "plain text".getBytes(), headers, null);

        var record = new Record(source, "key", "plain text");

        assertFalse(record.isValueIsJson());
        assertEquals("plain text", record.getValueCompressed());
    }

    @Test
    void shouldHandleNullKey() {
        var headers = new RecordHeaders();
        var source = new ConsumerRecord<byte[], byte[]>("topic", 0, 100L, 1234567890L,
                TimestampType.CREATE_TIME, -1, 20, null, "value".getBytes(), headers, null);

        var record = new Record(source, "", "value");

        assertTrue(record.isKeyIsNull());
        assertEquals(0, record.getKeySize());
    }

    @Test
    void shouldHandleNullValue() {
        var headers = new RecordHeaders();
        var source = new ConsumerRecord<byte[], byte[]>("topic", 0, 100L, 1234567890L,
                TimestampType.CREATE_TIME, 10, -1, "key".getBytes(), null, headers, null);

        var record = new Record(source, "key", "");

        assertTrue(record.isValueIsNull());
        assertEquals(0, record.getValueSize());
    }

    @Test
    void shouldExtractHeaders() {
        var headers = new RecordHeaders();
        headers.add("header1", "value1".getBytes());
        headers.add("header2", "value2".getBytes());
        var source = new ConsumerRecord<>("topic", 0, 100L, 1234567890L,
                TimestampType.CREATE_TIME, 10, 20, "key".getBytes(), "value".getBytes(), headers, null);

        var record = new Record(source, "key", "value");

        assertEquals(2, record.getHeaders().size());
        assertEquals("value1", record.getHeaders().get("header1"));
        assertEquals("value2", record.getHeaders().get("header2"));
    }

    @Test
    void shouldPrettyPrintJson() {
        var headers = new RecordHeaders();
        var source = new ConsumerRecord<>("topic", 0, 100L, 1234567890L,
                TimestampType.CREATE_TIME, 10, 20, "key".getBytes(), "{\"a\":1}".getBytes(), headers, null);

        var record = new Record(source, "key", "{\"a\":1}");

        assertTrue(record.getValueFormatted().contains("\n"));
    }

    @Test
    void shouldCompressJsonWithWhitespace() {
        var headers = new RecordHeaders();
        var jsonWithWhitespace = "{\n  \"name\": \"value\"\n}";
        var source = new ConsumerRecord<>("topic", 0, 100L, 1234567890L,
                TimestampType.CREATE_TIME, 10, 50, "key".getBytes(), jsonWithWhitespace.getBytes(), headers, null);

        var record = new Record(source, "key", jsonWithWhitespace);

        assertTrue(record.isValueIsJson());
        assertFalse(record.getValueCompressed().contains("\n"));
    }

    @Test
    void shouldConvertToDto() {
        var headers = new RecordHeaders();
        var source = new ConsumerRecord<>("test-topic", 2, 500L, 9876543210L,
                TimestampType.CREATE_TIME, 10, 20, "key".getBytes(), "value".getBytes(), headers, null);

        var record = new Record(source, "key", "value");
        var dto = record.toDto();

        assertEquals("test-topic", dto.get("topic"));
        assertEquals(2, dto.get("partition"));
        assertEquals(500L, dto.get("offset"));
        assertEquals(9876543210L, dto.get("timestamp"));
    }

    @Test
    void shouldReturnNullKeyInDtoWhenKeyIsNull() {
        var headers = new RecordHeaders();
        var source = new ConsumerRecord<byte[], byte[]>("topic", 0, 100L, 1234567890L,
                TimestampType.CREATE_TIME, -1, 20, null, "value".getBytes(), headers, null);

        var record = new Record(source, "", "value");
        var dto = record.toDto();

        assertNull(dto.get("key"));
    }

    @Test
    void shouldParseJsonKeyInDto() {
        var headers = new RecordHeaders();
        var source = new ConsumerRecord<>("topic", 0, 100L, 1234567890L,
                TimestampType.CREATE_TIME, 20, 20, "{\"id\":123}".getBytes(), "value".getBytes(), headers, null);

        var record = new Record(source, "{\"id\":123}", "value");
        var dto = record.toDto();

        assertNotNull(dto.get("key"));
        assertTrue(record.isKeyIsJson());
    }

    @Test
    void shouldPreservePartitionAndOffsetInfo() {
        var headers = new RecordHeaders();
        var source = new ConsumerRecord<>("topic", 5, 12345L, 1234567890L,
                TimestampType.CREATE_TIME, 10, 20, "key".getBytes(), "value".getBytes(), headers, null);

        var record = new Record(source, "key", "value");

        assertEquals(5, record.getPartition());
        assertEquals(12345L, record.getOffset());
        assertEquals("topic", record.getTopicName());
    }

    @Test
    void shouldHandleLASTStaticInstance() {
        var lastRecord = Record.LAST;

        assertTrue(lastRecord.isLast());
        assertNull(lastRecord.getTopicName());
        assertEquals(0, lastRecord.getPartition());
        assertEquals(0, lastRecord.getOffset());
    }

    @Test
    void shouldHandleDuplicateHeaderKeys() {
        var headers = new RecordHeaders();
        headers.add("dup", "first".getBytes());
        headers.add("dup", "second".getBytes());
        var source = new ConsumerRecord<>("topic", 0, 100L, 1234567890L,
                TimestampType.CREATE_TIME, 10, 20, "key".getBytes(), "value".getBytes(), headers, null);

        var record = new Record(source, "key", "value");

        assertEquals(1, record.getHeaders().size());
        assertEquals("first", record.getHeaders().get("dup"));
    }

    @Test
    void shouldHandleInvalidJsonGracefully() {
        var headers = new RecordHeaders();
        var invalidJson = "{not valid json}";
        var source = new ConsumerRecord<>("topic", 0, 100L, 1234567890L,
                TimestampType.CREATE_TIME, 10, 20, "key".getBytes(), invalidJson.getBytes(), headers, null);

        var record = new Record(source, "key", invalidJson);

        assertTrue(record.isValueIsJson());
        assertEquals("{not valid json}", record.getValueCompressed());
    }
}
