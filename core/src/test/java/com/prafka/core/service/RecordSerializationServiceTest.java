package com.prafka.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.prafka.core.manager.SerDeManager;
import com.prafka.core.model.NewRecord;
import com.prafka.core.model.SerdeType;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import org.apache.kafka.common.serialization.Serializer;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecordSerializationServiceTest {

    private SerDeManager serDeManager = mock(SerDeManager.class);
    private SchemaRegistryClient schemaRegistryClient = mock(SchemaRegistryClient.class);
    private RecordSerializationService recordSerializationService = new RecordSerializationService(serDeManager) {
        @Override
        protected SchemaRegistryClient schemaRegistryClient(String clusterId) {
            return schemaRegistryClient;
        }
    };

    @Test
    void shouldSerializeByDefaultSerde() {
        // Given
        var clusterId = "test-cluster";
        var topicName = "test-topic";
        var key = "test-key";
        var value = "test-value";

        var record = new NewRecord();
        record.setKey(key);
        record.setValue(value);
        record.setHeaders(Map.of("header-key", "header-value"));

        // When
        var result = recordSerializationService.serialize(clusterId, topicName, record);

        // Then
        assertEquals(topicName, result.topic());
        assertArrayEquals(key.getBytes(StandardCharsets.UTF_8), result.key());
        assertArrayEquals(value.getBytes(StandardCharsets.UTF_8), result.value());
        var header =result.headers().iterator().next();
        assertEquals("header-key", header.key());
        assertNotNull(header.value());
    }

    @Test
    void shouldSerializeBySchemaRegistry() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var topicName = "test-topic";
        var key = "{\"name\":\"test-key\"}";
        var value = "{\"id\":123,\"message\":\"test-value\"}";

        var record = new NewRecord();
        record.setKey(key);
        record.setValue(value);
        record.setKeySerde(SerdeType.SCHEMA_REGISTRY);
        record.setValueSerde(SerdeType.SCHEMA_REGISTRY);
        record.setKeySchemaSubject(Optional.of("key-schema"));
        record.setValueSchemaSubject(Optional.of("value-schema"));
        record.setHeaders(Map.of("header-key", "header-value"));

        var schemaMetadata = mock(SchemaMetadata.class);
        when(schemaMetadata.getId()).thenReturn(1);

        var schema = mock(JsonSchema.class);
        when(schema.schemaType()).thenReturn("JSON");
        when(schema.validate(any(JsonNode.class))).thenReturn(mock(JsonNode.class));

        when(schemaRegistryClient.getLatestSchemaMetadata("key-schema")).thenReturn(schemaMetadata);
        when(schemaRegistryClient.getLatestSchemaMetadata("value-schema")).thenReturn(schemaMetadata);
        when(schemaRegistryClient.getSchemaBySubjectAndId("key-schema", 1)).thenReturn(schema);
        when(schemaRegistryClient.getSchemaBySubjectAndId("value-schema", 1)).thenReturn(schema);

        var keySerializer = mock(Serializer.class);
        var valueSerializer = mock(Serializer.class);
        when(keySerializer.serialize(any(), any())).thenReturn("serialized-key".getBytes());
        when(valueSerializer.serialize(any(), any())).thenReturn("serialized-value".getBytes());

        when(serDeManager.jsonSer(clusterId, schema, true)).thenReturn(keySerializer);
        when(serDeManager.jsonSer(clusterId, schema, false)).thenReturn(valueSerializer);

        // When
        var result = recordSerializationService.serialize(clusterId, topicName, record);

        // Then
        assertEquals(topicName, result.topic());
        assertNotNull(result.key());
        assertNotNull(result.value());
        assertEquals(1, result.headers().toArray().length);
    }
}