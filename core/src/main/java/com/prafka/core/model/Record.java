package com.prafka.core.model;

import com.google.gson.JsonParser;
import com.prafka.core.util.CollectionUtils;
import com.prafka.core.util.JsonFactory;
import lombok.Getter;
import org.apache.commons.lang3.RegExUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.record.TimestampType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a Kafka record (message) with its key, value, headers, and metadata.
 *
 * <p>This class wraps consumed records and provides both raw and formatted versions
 * of the key and value. JSON content is automatically detected and can be retrieved
 * in compressed (single-line) or pretty-printed format.
 *
 * @see ConsumerRecord
 * @see NewRecord
 */
@Getter
public class Record {

    private final long timestamp;
    private final TimestampType timestampType;
    private final String topicName;
    private final int partition;
    private final long offset;
    private final String key;
    private final int keySize;
    private final boolean keyIsNull;
    private final boolean keyIsJson;
    private final String keyCompressed;
    private final String keyFormatted;
    private final String value;
    private final int valueSize;
    private final boolean valueIsNull;
    private final boolean valueIsJson;
    private final String valueCompressed;
    private final String valueFormatted;
    private final Map<String, String> headers;
    private final boolean last;

    public Record(ConsumerRecord<byte[], byte[]> source, String key, String value) {
        timestamp = source.timestamp();
        timestampType = source.timestampType();
        topicName = source.topic();
        partition = source.partition();
        offset = source.offset();
        this.key = key;
        keySize = Math.max(source.serializedKeySize(), 0);
        keyIsNull = source.key() == null;
        keyIsJson = isJson(key);
        keyCompressed = toCompressed(key, keyIsJson);
        keyFormatted = toFormatted(key, keyIsJson);
        this.value = value;
        valueSize = Math.max(source.serializedValueSize(), 0);
        valueIsNull = source.value() == null;
        valueIsJson = isJson(value);
        valueCompressed = toCompressed(value, valueIsJson);
        valueFormatted = toFormatted(value, valueIsJson);
        headers = new HashMap<>();
        for (var header : source.headers().toArray()) {
            headers.putIfAbsent(header.key(), new String(header.value()));
        }
        last = false;
    }

    public Record(NewRecord source, RecordMetadata metadata) {
        timestamp = metadata.timestamp();
        timestampType = TimestampType.CREATE_TIME; // todo provide other options
        topicName = metadata.topic();
        partition = metadata.partition();
        offset = metadata.offset();
        key = source.getKey();
        keySize = metadata.serializedKeySize();
        keyIsNull = source.getKeySerde() == SerdeType.NULL;
        keyIsJson = isJson(key);
        keyCompressed = toCompressed(source.getKey(), keyIsJson);
        keyFormatted = toFormatted(source.getKey(), keyIsJson);
        value = source.getValue();
        valueSize = metadata.serializedValueSize();
        valueIsNull = source.getValueSerde() == SerdeType.NULL;
        valueIsJson = isJson(value);
        valueCompressed = toCompressed(source.getValue(), valueIsJson);
        valueFormatted = toFormatted(source.getValue(), valueIsJson);
        headers = source.getHeaders();
        last = false;
    }

    public static Record LAST = new Record();

    private Record() {
        timestamp = 0;
        timestampType = TimestampType.NO_TIMESTAMP_TYPE;
        topicName = null;
        partition = 0;
        offset = 0;
        key = null;
        keySize = 0;
        keyIsNull = true;
        keyIsJson = false;
        keyCompressed = null;
        keyFormatted = null;
        value = null;
        valueSize = 0;
        valueIsNull = true;
        valueIsJson = false;
        valueCompressed = null;
        valueFormatted = null;
        headers = Collections.emptyMap();
        last = true;
    }

    public Map<Object, Object> toDto() {
        return CollectionUtils.mapOf(
                "topic", topicName,
                "partition", getPartition(),
                "offset", getOffset(),
                "timestamp", getTimestamp(),
                "key", keyIsNull ? null : (keyIsJson ? JsonFactory.gsonDefault.fromJson(getKeyCompressed(), JsonFactory.MAP_STING_OBJECT_TYPE) : getKeyFormatted()),
                "keySize", getKeySize(),
                "value", valueIsNull ? null : (valueIsJson ? JsonFactory.gsonDefault.fromJson(getValueCompressed(), JsonFactory.MAP_STING_OBJECT_TYPE) : getValueFormatted()),
                "valueSize", getValueSize(),
                "headers", getHeaders()
        );
    }

    private static String trim(String payload) {
        return RegExUtils.removeAll(payload.trim(), "[\r\n]");
    }

    private static boolean isJson(String payload) {
        var trimmed = trim(payload);
        return (trimmed.startsWith("[") && trimmed.endsWith("]")) || (trimmed.startsWith("{") && trimmed.endsWith("}"));
    }

    private static String toCompressed(String payload, boolean isJson) {
        var trimmed = trim(payload);
        if (isJson) {
            try {
                return JsonFactory.gsonDefault.toJson(JsonParser.parseString(trimmed));
            } catch (Exception ignored) {
                return trimmed;
            }
        }
        return trimmed;
    }

    private static String toFormatted(String payload, boolean isJson) {
        if (isJson) {
            try {
                return JsonFactory.gsonPretty.toJson(JsonParser.parseString(payload));
            } catch (Exception ignored) {
                return payload;
            }
        }
        return payload;
    }
}
