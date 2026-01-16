package com.prafka.core.manager;

import com.google.protobuf.Message;
import com.prafka.core.model.SerdeType;
import com.prafka.core.service.AbstractService;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaUtils;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.SeekableByteArrayInput;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Named
@Singleton
public class SerDeManager extends AbstractService implements Closeable {

    private static final Map<Pair<String, Boolean>, Deserializer<Object>> AVRO_DESER = new ConcurrentHashMap<>();
    private static final Map<Pair<String, Boolean>, Deserializer<Object>> JSON_DESER = new ConcurrentHashMap<>();
    private static final Map<Pair<String, Boolean>, Deserializer<Message>> PROTOBUF_DESER = new ConcurrentHashMap<>();
    private static final Map<SerdeType, Serde<?>> STANDARD_SERDE = Map.of(
            SerdeType.STRING, new Serdes.StringSerde(),
            SerdeType.BYTES, new Serdes.BytesSerde(),
            SerdeType.SHORT, new Serdes.ShortSerde(),
            SerdeType.INTEGER, new Serdes.IntegerSerde(),
            SerdeType.LONG, new Serdes.LongSerde(),
            SerdeType.FLOAT, new Serdes.FloatSerde(),
            SerdeType.DOUBLE, new Serdes.DoubleSerde(),
            SerdeType.UUID, new Serdes.UUIDSerde(),
            SerdeType.AVRO, new AvroEmbeddedSerde()
    );

    public Serializer<Object> avroSer(String clusterId, boolean isKey) {
        var instance = new KafkaAvroSerializer(schemaRegistryClient(clusterId));
        instance.configure(getDefaultSchemaRegistryConfig(), isKey);
        return instance;
    }

    public Serializer<Object> jsonSer(String clusterId, JsonSchema schema, boolean isKey) {
        var instance = new CustomKafkaJsonSchemaSerializer<>(schemaRegistryClient(clusterId), schema, isKey);
        instance.configure(getDefaultSchemaRegistryConfig(), isKey);
        return instance;
    }

    public Serializer<Message> protobufSer(String clusterId, boolean isKey) {
        var instance = new KafkaProtobufSerializer<>(schemaRegistryClient(clusterId));
        instance.configure(getDefaultSchemaRegistryConfig(), isKey);
        return instance;
    }

    public <T> Serializer<T> standardSer(SerdeType type) {
        //noinspection unchecked
        return (Serializer<T>) STANDARD_SERDE.get(type).serializer();
    }

    public Deserializer<Object> avroDeser(String clusterId, boolean isKey) {
        return AVRO_DESER.computeIfAbsent(Pair.of(clusterId, isKey), it -> {
            var instance = new KafkaAvroDeserializer(schemaRegistryClient(clusterId));
            instance.configure(getDefaultSchemaRegistryConfig(), isKey);
            return instance;
        });
    }

    public Deserializer<Object> jsonDeser(String clusterId, boolean isKey) {
        return JSON_DESER.computeIfAbsent(Pair.of(clusterId, isKey), it -> {
            var instance = new KafkaJsonSchemaDeserializer<>(schemaRegistryClient(clusterId));
            instance.configure(getDefaultSchemaRegistryConfig(), isKey);
            return instance;
        });
    }

    public Deserializer<Message> protobufDeser(String clusterId, boolean isKey) {
        return PROTOBUF_DESER.computeIfAbsent(Pair.of(clusterId, isKey), it -> {
            var instance = new KafkaProtobufDeserializer<>(schemaRegistryClient(clusterId));
            instance.configure(getDefaultSchemaRegistryConfig(), isKey);
            return instance;
        });
    }

    public Deserializer<?> standardDeser(SerdeType type) {
        return STANDARD_SERDE.get(type).deserializer();
    }

    private static Map<String, ?> getDefaultSchemaRegistryConfig() {
        return Map.of(
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "empty",
                AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, false,
                AbstractKafkaSchemaSerDeConfig.USE_LATEST_VERSION, true
        );
    }

    @Override
    public void close() {
        AVRO_DESER.clear();
        JSON_DESER.clear();
        PROTOBUF_DESER.clear();
    }

    @Override
    public void close(String clusterId) {
        closeDeserIfPresent(AVRO_DESER.remove(Pair.of(clusterId, true)));
        closeDeserIfPresent(AVRO_DESER.remove(Pair.of(clusterId, false)));
        closeDeserIfPresent(JSON_DESER.remove(Pair.of(clusterId, true)));
        closeDeserIfPresent(JSON_DESER.remove(Pair.of(clusterId, false)));
        closeDeserIfPresent(PROTOBUF_DESER.remove(Pair.of(clusterId, true)));
        closeDeserIfPresent(PROTOBUF_DESER.remove(Pair.of(clusterId, false)));
    }

    private void closeDeserIfPresent(Deserializer<?> deser) {
        Optional.ofNullable(deser).ifPresent(Deserializer::close);
    }

    private static class AvroEmbeddedSerde extends Serdes.WrapperSerde<String> {

        public AvroEmbeddedSerde() {
            super(new AvroEmbeddedSerializer(), new AvroEmbeddedDeserializer());
        }

        private static class AvroEmbeddedSerializer implements Serializer<String> {
            @Override
            public byte[] serialize(String topic, String data) {
                throw new UnsupportedOperationException();
            }
        }

        private static class AvroEmbeddedDeserializer implements Deserializer<String> {
            @Override
            public String deserialize(String topic, byte[] data) {
                try (var reader = new DataFileReader<>(new SeekableByteArrayInput(data), new GenericDatumReader<>())) {
                    if (!reader.hasNext()) return null;
                    return new String(AvroSchemaUtils.toJson(reader.next()));
                } catch (Exception e) {
                    throw new SerializationException(e.getMessage(), e);
                }
            }
        }
    }

    private static class CustomKafkaJsonSchemaSerializer<T> extends KafkaJsonSchemaSerializer<T> {

        private final JsonSchema schema;
        private final boolean isKey;

        public CustomKafkaJsonSchemaSerializer(SchemaRegistryClient client, JsonSchema schema, boolean isKey) {
            super(client);
            this.schema = schema;
            this.isKey = isKey;
        }

        @Override
        public byte[] serialize(String topic, T record) {
            if (record == null) return null;
            return serializeImpl(getSubjectName(topic, isKey, record, schema), record, schema);
        }
    }
}
