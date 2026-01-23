package com.prafka.core.service;

import com.google.protobuf.util.JsonFormat;
import com.prafka.core.manager.SerDeManager;
import com.prafka.core.model.NewRecord;
import com.prafka.core.model.Schema;
import com.prafka.core.model.SerdeType;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaUtils;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static com.prafka.core.util.JsonFactory.objectMapperDefault;

/**
 * Service for serializing Kafka record keys and values.
 *
 * <p>Supports serialization using Schema Registry (Avro, JSON Schema, Protobuf)
 * and standard Kafka serializers (String, numeric types, UUID). Converts
 * {@link NewRecord} instances into {@link ProducerRecord} with serialized payloads.
 *
 * @see NewRecord
 * @see SerdeType
 * @see SerDeManager
 */
@Named
@Singleton
public class RecordSerializationService extends AbstractService {

    private final SerDeManager serDeManager;

    @Inject
    public RecordSerializationService(SerDeManager serDeManager) {
        this.serDeManager = serDeManager;
    }

    public ProducerRecord<byte[], byte[]> serialize(String clusterId, String topicName, NewRecord record) {
        return new ProducerRecord<>(
                topicName,
                record.getPartition().orElse(null),
                record.getTimestamp().orElse(null),
                serialize(clusterId, topicName, record.getKey(), record.getKeySerde(), record.getKeySchemaSubject(), true),
                serialize(clusterId, topicName, record.getValue(), record.getValueSerde(), record.getValueSchemaSubject(), false),
                serializeHeaders(record)
        );
    }

    private byte[] serialize(String clusterId, String topicName, String payload, SerdeType serde, Optional<String> schemaSubject, boolean isKey) {
        if (payload == null || serde == SerdeType.NULL) return null;
        if (serde == SerdeType.SCHEMA_REGISTRY && schemaSubject.isPresent()) {
            return serializeBySchemaRegistry(clusterId, topicName, payload, schemaSubject.get(), isKey);
        }
        return switch (serde) {
            case SHORT -> serDeManager.<Short>standardSer(serde).serialize(topicName, Short.parseShort(payload));
            case INTEGER -> serDeManager.<Integer>standardSer(serde).serialize(topicName, Integer.parseInt(payload));
            case LONG -> serDeManager.<Long>standardSer(serde).serialize(topicName, Long.parseLong(payload));
            case FLOAT -> serDeManager.<Float>standardSer(serde).serialize(topicName, Float.parseFloat(payload));
            case DOUBLE -> serDeManager.<Double>standardSer(serde).serialize(topicName, Double.parseDouble(payload));
            case UUID -> serDeManager.<UUID>standardSer(serde).serialize(topicName, UUID.fromString(payload));
            default -> payload.getBytes(StandardCharsets.UTF_8);
        };
    }

    private byte[] serializeBySchemaRegistry(String clusterId, String topicName, String payload, String schemaSubject, boolean isKey) {
        try {
            var schemaId = schemaRegistryClient(clusterId).getLatestSchemaMetadata(schemaSubject).getId();
            var schema = schemaRegistryClient(clusterId).getSchemaBySubjectAndId(schemaSubject, schemaId);
            switch (Schema.Type.valueOf(schema.schemaType())) {
                case AVRO -> {
                    var data = AvroSchemaUtils.toObject(payload, (AvroSchema) schema);
                    return serDeManager.avroSer(clusterId, isKey).serialize(topicName, data);
                }
                case JSON -> {
                    var data = objectMapperDefault.readTree(payload);
                    ((JsonSchema) schema).validate(data);
                    return serDeManager.jsonSer(clusterId, (JsonSchema) schema, isKey).serialize(topicName, data);
                }
                case PROTOBUF -> {
                    var builder = ((ProtobufSchema) schema).newMessageBuilder();
                    JsonFormat.parser().merge(payload, builder);
                    var data = builder.build();
                    return serDeManager.protobufSer(clusterId, isKey).serialize(topicName, data);
                }
            }
        } catch (Exception e) {
            logDebugError(e);
            throw new RuntimeException(e);
        }
        throw new IllegalStateException();
    }

    private Headers serializeHeaders(NewRecord record) {
        var headers = new RecordHeaders();
        record.getHeaders().forEach((k, v) -> {
            if (StringUtils.isNotBlank(k)) {
                headers.add(k, StringUtils.getBytes(v, StandardCharsets.UTF_8));
            }
        });
        return headers;
    }
}
