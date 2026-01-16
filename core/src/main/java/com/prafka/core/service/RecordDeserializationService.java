package com.prafka.core.service;

import com.google.protobuf.util.JsonFormat;
import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import com.prafka.core.manager.SerDeManager;
import com.prafka.core.model.Schema;
import com.prafka.core.model.SerdeType;
import com.prafka.core.model.Topic;
import com.prafka.core.util.CollectionUtils;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaUtils;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.protocol.ByteBufferAccessor;
import org.apache.kafka.common.record.RecordBatch;
import org.apache.kafka.common.requests.OffsetCommitRequest;
import org.apache.kafka.coordinator.group.generated.GroupMetadataKey;
import org.apache.kafka.coordinator.group.generated.GroupMetadataValue;
import org.apache.kafka.coordinator.group.generated.OffsetCommitKey;
import org.apache.kafka.coordinator.group.generated.OffsetCommitValue;
import org.apache.kafka.coordinator.transaction.generated.TransactionLogKey;
import org.apache.kafka.coordinator.transaction.generated.TransactionLogValue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

import static com.prafka.core.util.JsonFactory.gsonDefault;

@Named
@Singleton
public class RecordDeserializationService extends AbstractService {

    private final SerDeManager serDeManager;

    @Inject
    public RecordDeserializationService(SerDeManager serDeManager) {
        this.serDeManager = serDeManager;
    }

    public Pair<String, String> deserialize(String clusterId, Topic topic, ConsumerRecord<byte[], byte[]> record, SerdeType keySerde, SerdeType valueSerde) {
        return Pair.of(
                deserialize(clusterId, topic, record, keySerde, valueSerde, true),
                deserialize(clusterId, topic, record, keySerde, valueSerde, false)
        );
    }

    private String deserialize(String clusterId, Topic topic, ConsumerRecord<byte[], byte[]> record, SerdeType keySerde, SerdeType valueSerde, boolean isKey) {
        var payload = isKey ? record.key() : record.value();
        if (payload == null) return "null";

        var payloadSerde = isKey ? keySerde : valueSerde;
        if (payloadSerde != SerdeType.AUTO) {
            try {
                return serDeManager.standardDeser(payloadSerde).deserialize(topic.getName(), record.headers(), payload).toString();
            } catch (SerializationException e) {
                var msg = e.getMessage();
                return msg == null ? "null" : msg;
            }
        }

        if (topic.isInternal()) {
            if (topic.getName().equals("__consumer_offsets")) {
                var deserialized = deserializeByConsumerOffsets(clusterId, topic, record, isKey);
                if (deserialized.isPresent()) {
                    return deserialized.get();
                }
            }
            if (topic.getName().equals("__transaction_state")) {
                var deserialized = deserializeByTransactionState(clusterId, topic, record, isKey);
                if (deserialized.isPresent()) {
                    return deserialized.get();
                }
            }
        }

        var schemaId = getSchemaId(payload);
        if (schemaId != null && kafkaManager.schemaRegistryIsDefined(clusterId)) {
            var deserialized = deserializeBySchemaRegistry(clusterId, topic, payload, isKey, schemaId);
            if (deserialized.isPresent()) {
                return deserialized.get();
            }
        }

        // todo add support for xml, smile, messagepack, custom

        if (isUtf8(payload)) {
            return new String(payload);
        }

        return HexFormat.ofDelimiter(" ").formatHex(payload);
    }

    private Optional<String> deserializeBySchemaRegistry(String clusterId, Topic topic, byte[] payload, boolean isKey, int schemaId) {
        try {
            var schema = schemaRegistryClient(clusterId).getSchemaById(schemaId);
            switch (Schema.Type.valueOf(schema.schemaType())) {
                case AVRO -> {
                    var deserialized = serDeManager.avroDeser(clusterId, isKey).deserialize(topic.getName(), payload);
                    return Optional.of(new String(AvroSchemaUtils.toJson(deserialized)));
                }
                case JSON -> {
                    var deserialized = serDeManager.jsonDeser(clusterId, isKey).deserialize(topic.getName(), payload);
                    return Optional.of(String.valueOf(deserialized));
                }
                case PROTOBUF -> {
                    var deserialized = serDeManager.protobufDeser(clusterId, isKey).deserialize(topic.getName(), payload);
                    return Optional.of(JsonFormat.printer().print(deserialized));
                }
            }
        } catch (Exception e) {
            logDebugError(e);
        }
        return Optional.empty();
    }

    private Optional<String> deserializeByConsumerOffsets(String clusterId, Topic topic, ConsumerRecord<byte[], byte[]> record, boolean isKey) {
        try {
            var keyBuffer = ByteBuffer.wrap(record.key());
            var keyVersion = keyBuffer.getShort();
            Object key;
            if (keyVersion >= OffsetCommitKey.LOWEST_SUPPORTED_VERSION && keyVersion <= OffsetCommitKey.HIGHEST_SUPPORTED_VERSION) {
                key = new OffsetCommitKey(new ByteBufferAccessor(keyBuffer), keyVersion);
            } else if (keyVersion >= GroupMetadataKey.LOWEST_SUPPORTED_VERSION && keyVersion <= GroupMetadataKey.HIGHEST_SUPPORTED_VERSION) {
                key = new GroupMetadataKey(new ByteBufferAccessor(keyBuffer), keyVersion);
            } else {
                throw new IllegalStateException();
            }
            if (isKey) {
                if (key instanceof OffsetCommitKey offsetKey) {
                    return Optional.of(toJson(CollectionUtils.mapOf(
                            "version", keyVersion,
                            "group", offsetKey.group(),
                            "topic", offsetKey.topic(),
                            "partition", offsetKey.partition()
                    )));
                }
                return Optional.of(toJson(CollectionUtils.mapOf("version", keyVersion, "key", ((GroupMetadataKey) key).group())));
            }

            var valueBuffer = ByteBuffer.wrap(record.value());
            var valueVersion = valueBuffer.getShort();
            if (keyVersion >= OffsetCommitKey.LOWEST_SUPPORTED_VERSION && keyVersion <= OffsetCommitKey.HIGHEST_SUPPORTED_VERSION) {
                if (valueVersion >= OffsetCommitValue.LOWEST_SUPPORTED_VERSION && valueVersion <= OffsetCommitValue.HIGHEST_SUPPORTED_VERSION) {
                    var value = new OffsetCommitValue(new ByteBufferAccessor(valueBuffer), valueVersion);
                    return Optional.of(toJson(CollectionUtils.mapOf(
                            "offset", value.offset(),
                            "leaderEpoch", (value.leaderEpoch() == RecordBatch.NO_PARTITION_LEADER_EPOCH) ? null : value.leaderEpoch(),
                            "metadata", value.metadata(),
                            "commitTimestamp", value.commitTimestamp(),
                            "expireTimestamp", (value.expireTimestamp() == OffsetCommitRequest.DEFAULT_TIMESTAMP) ? null : value.expireTimestamp()
                    )));
                } else {
                    throw new IllegalStateException();
                }
            }
            if (keyVersion >= GroupMetadataKey.LOWEST_SUPPORTED_VERSION && keyVersion <= GroupMetadataKey.HIGHEST_SUPPORTED_VERSION) {
                if (valueVersion >= GroupMetadataValue.LOWEST_SUPPORTED_VERSION && valueVersion <= GroupMetadataValue.HIGHEST_SUPPORTED_VERSION) {
                    var value = new GroupMetadataValue(new ByteBufferAccessor(valueBuffer), valueVersion);
                    return Optional.of(toJson(CollectionUtils.mapOf(
                            "group", ((GroupMetadataKey) key).group(),
                            "generation", value.generation(),
                            "protocolType", value.protocolType(),
                            "protocol", value.protocol(),
                            "leader", value.leader(),
                            "currentStateTimestamp", (value.currentStateTimestamp() == -1) ? null : value.currentStateTimestamp(),
                            "members", value.members().stream()
                                    .map(member -> CollectionUtils.mapOf(
                                            "memberId", member.memberId(),
                                            "groupInstanceId", member.groupInstanceId(),
                                            "clientId", member.clientId(),
                                            "clientHost", member.clientHost(),
                                            "sessionTimeoutMs", member.sessionTimeout(),
                                            "rebalanceTimeoutMs", (valueVersion == 0) ? member.sessionTimeout() : member.rebalanceTimeout()
                                    ))
                                    .toList()
                    )));
                } else {
                    throw new IllegalStateException();
                }
            }
        } catch (Exception e) {
            logDebugError(e);
        }
        return Optional.empty();
    }

    private Optional<String> deserializeByTransactionState(String clusterId, Topic topic, ConsumerRecord<byte[], byte[]> record, boolean isKey) {
        try {
            var keyBuffer = ByteBuffer.wrap(record.key());
            var keyVersion = keyBuffer.getShort();
            TransactionLogKey key;
            if (keyVersion >= TransactionLogKey.LOWEST_SUPPORTED_VERSION && keyVersion <= TransactionLogKey.HIGHEST_SUPPORTED_VERSION) {
                key = new TransactionLogKey(new ByteBufferAccessor(keyBuffer), keyVersion);
            } else {
                throw new IllegalStateException();
            }
            if (isKey) {
                return Optional.of(toJson(CollectionUtils.mapOf("version", keyVersion, "transactionalId", key.transactionalId())));
            }

            var valueBuffer = ByteBuffer.wrap(record.value());
            var valueVersion = valueBuffer.getShort();
            if (valueVersion >= TransactionLogValue.LOWEST_SUPPORTED_VERSION && valueVersion <= TransactionLogValue.HIGHEST_SUPPORTED_VERSION) {
                var value = new TransactionLogValue(new ByteBufferAccessor(valueBuffer), valueVersion);
                var topicPartitions = (value.transactionPartitions() == null) ? null : value.transactionPartitions().stream()
                        .flatMap(partitionsSchema ->
                                partitionsSchema.partitionIds().stream()
                                        .map(partitionId -> CollectionUtils.mapOf("topic", partitionsSchema.topic(), "partition", partitionId))
                        )
                        .toList();
                return Optional.of(toJson(CollectionUtils.mapOf(
                        "transactionalId", key.transactionalId(),
                        "producerId", value.producerId(),
                        "previousProducerId", value.previousProducerId(),
                        "nextProducerId", value.nextProducerId(),
                        "producerEpoch", value.producerEpoch(),
                        "txnTimeoutMs", value.transactionTimeoutMs(),
                        "state", switch (value.transactionStatus()) {
                            case 0 -> "Empty";
                            case 1 -> "Ongoing";
                            case 2 -> "PrepareCommit";
                            case 3 -> "PrepareAbort";
                            case 4 -> "CompleteCommit";
                            case 5 -> "CompleteAbort";
                            case 6 -> "Dead";
                            case 7 -> "PrepareEpochFence";
                            default -> throw new IllegalStateException();
                        },
                        "topicPartitions", (value.transactionStatus() == 0) ? Collections.emptyList() : topicPartitions,
                        "txnStartTimestamp", value.transactionStartTimestampMs(),
                        "txnLastUpdateTimestamp", value.transactionLastUpdateTimestampMs(),
                        "clientTransactionVersion", value.clientTransactionVersion()
                )));
            }
        } catch (Exception e) {
            logDebugError(e);
        }
        return Optional.empty();
    }

    private static Integer getSchemaId(byte[] payload) {
        try {
            var buffer = ByteBuffer.wrap(payload);
            var magicByte = buffer.get();
            var schemaId = buffer.getInt();
            if (magicByte == 0 && schemaId > 0) {
                return schemaId;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static boolean isUtf8(byte[] payload) {
        try {
            var matches = new CharsetDetector().setText(payload).detectAll();
            for (CharsetMatch match : matches) {
                if (match.getName().equals(StandardCharsets.UTF_8.name())) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private String toJson(Map<?, ?> map) {
        return gsonDefault.toJson(map);
    }

    public Object tryToMap(String json) {
        try {
            return gsonDefault.fromJson(json, Map.class);
        } catch (Exception e) {
            return json;
        }
    }
}
