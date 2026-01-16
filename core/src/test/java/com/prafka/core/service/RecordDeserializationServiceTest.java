package com.prafka.core.service;

import com.prafka.core.manager.KafkaManager;
import com.prafka.core.manager.SerDeManager;
import com.prafka.core.model.SerdeType;
import com.prafka.core.model.Topic;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecordDeserializationServiceTest {

    private SerDeManager serDeManager = mock(SerDeManager.class);
    private KafkaManager kafkaManager = mock(KafkaManager.class);
    private SchemaRegistryClient schemaRegistryClient = mock(SchemaRegistryClient.class);
    private RecordDeserializationService recordDeserializationService = new RecordDeserializationService(serDeManager) {
        @Override
        protected SchemaRegistryClient schemaRegistryClient(String clusterId) {
            return schemaRegistryClient;
        }
    };

    @Test
    void shouldDeserializeByConsumerOffsets() {
        // Given
        var group = "test-group";
        var topic = new Topic(new TopicDescription("__consumer_offsets", true, Collections.emptyList()), Collections.emptyMap(), Collections.emptyMap());

        short keyVersion = 0;
        int partition = 1;

        var keyBuffer = ByteBuffer.allocate(1024);
        keyBuffer.putShort(keyVersion);
        keyBuffer.putShort((short) group.length());
        keyBuffer.put(group.getBytes());
        keyBuffer.putShort((short) topic.getName().length());
        keyBuffer.put(topic.getName().getBytes());
        keyBuffer.putInt(partition);
        keyBuffer.flip();

        var keyBytes = new byte[keyBuffer.remaining()];
        keyBuffer.get(keyBytes);

        short valueVersion = 0;
        long offset = 12345L;
        var metadata = "test metadata";
        long commitTimestamp = System.currentTimeMillis();
        long expireTimestamp = -1L;

        var valueBuffer = ByteBuffer.allocate(1024);
        valueBuffer.putShort(valueVersion);
        valueBuffer.putLong(offset);
        valueBuffer.putShort((short) metadata.length());
        valueBuffer.put(metadata.getBytes());
        valueBuffer.putLong(commitTimestamp);
        valueBuffer.putLong(expireTimestamp);
        valueBuffer.flip();

        byte[] valueBytes = new byte[valueBuffer.remaining()];
        valueBuffer.get(valueBytes);

        var record = new ConsumerRecord<>(topic.getName(), 0, 0, keyBytes, valueBytes);

        // When
        var result = recordDeserializationService.deserialize("cluster1", topic, record, SerdeType.AUTO, SerdeType.AUTO);

        // Then
        var keyResult = result.getKey();
        var valueResult = result.getValue();

        assertTrue(keyResult.contains("\"group\":\"test-group\""));
        assertTrue(keyResult.contains("\"topic\":\"__consumer_offsets\""));
        assertTrue(keyResult.contains("\"partition\":1"));

        assertTrue(valueResult.contains("\"offset\":12345"));
        assertTrue(valueResult.contains("\"metadata\":\"test metadata\""));
    }

    @Test
    void shouldDeserializeByTransactionState() {
        // Given
        var transactionalId = "test-transaction-id";
        var topic = new Topic(new TopicDescription("__transaction_state", true, Collections.emptyList()), Collections.emptyMap(), Collections.emptyMap());

        short keyVersion = 0;
        var keyBuffer = ByteBuffer.allocate(1024);
        keyBuffer.putShort(keyVersion);
        keyBuffer.putShort((short) transactionalId.length());
        keyBuffer.put(transactionalId.getBytes());
        keyBuffer.flip();

        var keyBytes = new byte[keyBuffer.remaining()];
        keyBuffer.get(keyBytes);

        short valueVersion = 0;
        long producerId = 12345L;
        short producerEpoch = 1;
        int txnTimeoutMs = 60000;
        byte txnState = 1;
        long txnLastUpdateTimestampMs = 60000L;
        long txnStartTimestampMs = 60000L;

        var valueBuffer = ByteBuffer.allocate(1024);
        valueBuffer.putShort(valueVersion);
        valueBuffer.putLong(producerId);
        valueBuffer.putShort(producerEpoch);
        valueBuffer.putInt(txnTimeoutMs);
        valueBuffer.put(txnState);
        valueBuffer.putInt(0);
        valueBuffer.putLong(txnLastUpdateTimestampMs);
        valueBuffer.putLong(txnStartTimestampMs);
        valueBuffer.flip();

        byte[] valueBytes = new byte[valueBuffer.remaining()];
        valueBuffer.get(valueBytes);

        var record = new ConsumerRecord<>(topic.getName(), 0, 0, keyBytes, valueBytes);

        // When
        var result = recordDeserializationService.deserialize("cluster1", topic, record, SerdeType.AUTO, SerdeType.AUTO);

        // Then
        var keyResult = result.getKey();
        var valueResult = result.getValue();

        assertTrue(keyResult.contains("\"transactionalId\":\"test-transaction-id\""));
        assertTrue(valueResult.contains("\"producerId\":12345"));
        assertTrue(valueResult.contains("\"producerEpoch\":1"));
        assertTrue(valueResult.contains("\"txnTimeoutMs\":60000"));
    }

    @Test
    void shouldDeserializeBySchemaRegistry() throws Exception {
        // Given
        var topic = new Topic(new TopicDescription("test-topic", false, Collections.emptyList()), Collections.emptyMap(), Collections.emptyMap());
        var clusterId = "cluster1";
        var keySchemaId = 1;
        var valueSchemaId = 2;

        var keyPayload = ByteBuffer.allocate(5);
        keyPayload.put((byte) 0);
        keyPayload.putInt(keySchemaId);
        var keyPayloadBytes = keyPayload.array();

        var valuePayload = ByteBuffer.allocate(5);
        valuePayload.put((byte) 0);
        valuePayload.putInt(valueSchemaId);
        var valuePayloadBytes = valuePayload.array();

        var keySchema = mock(ParsedSchema.class);
        when(keySchema.schemaType()).thenReturn("AVRO");
        when(schemaRegistryClient.getSchemaById(keySchemaId)).thenReturn(keySchema);

        var valueSchema = mock(ParsedSchema.class);
        when(valueSchema.schemaType()).thenReturn("AVRO");
        when(schemaRegistryClient.getSchemaById(valueSchemaId)).thenReturn(valueSchema);

        var keyAvroDeserializer = mock(org.apache.kafka.common.serialization.Deserializer.class);
        when(serDeManager.avroDeser(clusterId, true)).thenReturn(keyAvroDeserializer);

        var valueAvroDeserializer = mock(org.apache.kafka.common.serialization.Deserializer.class);
        when(serDeManager.avroDeser(clusterId, false)).thenReturn(valueAvroDeserializer);

        var keySchemaBuilder = org.apache.avro.SchemaBuilder.record("KeyRecord").fields()
                .name("id").type().longType().noDefault()
                .name("name").type().stringType().noDefault()
                .endRecord();

        var keyGenericRecord = new org.apache.avro.generic.GenericData.Record(keySchemaBuilder);
        keyGenericRecord.put("id", 123L);
        keyGenericRecord.put("name", "test-key");

        var valueSchemaBuilder = org.apache.avro.SchemaBuilder.record("ValueRecord").fields()
                .name("timestamp").type().longType().noDefault()
                .name("message").type().stringType().noDefault()
                .name("count").type().intType().noDefault()
                .endRecord();

        var valueGenericRecord = new org.apache.avro.generic.GenericData.Record(valueSchemaBuilder);
        valueGenericRecord.put("timestamp", 1640995200000L);
        valueGenericRecord.put("message", "test-message");
        valueGenericRecord.put("count", 42);

        when(keyAvroDeserializer.deserialize(topic.getName(), keyPayloadBytes)).thenReturn(keyGenericRecord);
        when(valueAvroDeserializer.deserialize(topic.getName(), valuePayloadBytes)).thenReturn(valueGenericRecord);

        when(kafkaManager.schemaRegistryIsDefined(clusterId)).thenReturn(true);
        recordDeserializationService.setKafkaManager(kafkaManager);

        var consumerRecord = new org.apache.kafka.clients.consumer.ConsumerRecord<>(topic.getName(), 0, 0, keyPayloadBytes, valuePayloadBytes);

        // When
        var result = recordDeserializationService.deserialize(clusterId, topic, consumerRecord, SerdeType.AUTO, SerdeType.AUTO);

        // Then
        var keyResult = result.getKey();
        var valueResult = result.getValue();

        assertTrue(keyResult.contains("\"id\":123"));
        assertTrue(keyResult.contains("\"name\":\"test-key\""));

        assertTrue(valueResult.contains("\"timestamp\":1640995200000"));
        assertTrue(valueResult.contains("\"message\":\"test-message\""));
        assertTrue(valueResult.contains("\"count\":42"));
    }
}