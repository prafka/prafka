package com.prafka.core.service;

import com.prafka.core.model.ConsumeFilter;
import com.prafka.core.model.NewRecord;
import com.prafka.core.model.Record;
import com.prafka.core.model.Topic;
import com.prafka.core.util.StreamUtils;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.openjdk.nashorn.api.scripting.NashornScriptEngine;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for consuming and producing Kafka records.
 *
 * <p>Provides operations to consume records from topics with filtering support
 * (by partition, offset, timestamp, and JavaScript expressions) and to produce
 * new records with various serialization options.
 *
 * @see Record
 * @see NewRecord
 * @see ConsumeFilter
 */
@Named
@Singleton
public class RecordService extends AbstractService {

    private static final NashornScriptEngine NASHORN_SCRIPT_ENGINE = (NashornScriptEngine) new NashornScriptEngineFactory().getScriptEngine();

    private final TopicService topicService;
    private final RecordSerializationService serializationService;
    private final RecordDeserializationService deserializationService;

    @Inject
    public RecordService(TopicService topicService, RecordSerializationService serializationService, RecordDeserializationService deserializationService) {
        this.topicService = topicService;
        this.serializationService = serializationService;
        this.deserializationService = deserializationService;
    }

    public void consume(String clusterId, String topicName, ConsumeFilter filter, Consumer<Record> onRecord, AtomicBoolean cancel) {
        try {
            consume(clusterId, topicService.get(clusterId, topicName).get(), filter, onRecord, cancel);
        } catch (Throwable e) {
            onRecord.accept(Record.LAST);
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<Void> consume(String clusterId, String topicName, ConsumeFilter filter, Queue<Record> queue, AtomicBoolean cancel) {
        return topicService.get(clusterId, topicName)
                .thenAcceptAsync(topic -> consume(clusterId, topic, filter, queue::offer, cancel))
                .exceptionallyComposeAsync(it -> {
                    queue.offer(Record.LAST);
                    return CompletableFuture.failedStage(it);
                });
    }

    /**
     * Consumes records from a Kafka topic with filtering and delivers them via callback.
     *
     * <p>This method performs the following steps:
     * <ol>
     *   <li>Filters partitions based on the filter criteria (specific partitions or all non-empty)</li>
     *   <li>Determines starting offsets based on the filter's "from" type:
     *     <ul>
     *       <li>{@code BEGIN} - starts from the earliest offset in each partition</li>
     *       <li>{@code END} - starts from recent offsets to fetch approximately maxResults records</li>
     *       <li>{@code OFFSET} - starts from a specific offset (only partitions containing that offset)</li>
     *       <li>{@code DATETIME/TIMESTAMP} - starts from offsets corresponding to the given timestamp</li>
     *     </ul>
     *   </li>
     *   <li>Polls records in batches (max 100 per poll) with 1-second timeout</li>
     *   <li>Deserializes each record's key and value using the specified serde types</li>
     *   <li>Applies JavaScript filter expressions (if any) using Nashorn engine with bindings:
     *     {@code key}, {@code value}, {@code headers}, {@code offset}, {@code partition}, {@code timestamp}</li>
     *   <li>Delivers matching records to the callback until maxResults is reached or no more data</li>
     *   <li>Sends {@link Record#LAST} as the final callback to signal completion</li>
     * </ol>
     *
     * <p>The polling loop terminates when any of the following conditions is met:
     * <ul>
     *   <li>The requested number of records (maxResults) has been delivered</li>
     *   <li>All partitions have been consumed up to their end offsets</li>
     *   <li>Three consecutive empty polls occur</li>
     *   <li>The cancel flag is set to true</li>
     *   <li>A safety limit of 10 poll iterations is reached</li>
     * </ul>
     *
     * @param clusterId the cluster identifier
     * @param topic     the topic metadata including partition and offset information
     * @param filter    the filter criteria specifying partitions, offsets, serdes, and expressions
     * @param onRecord  callback invoked for each matching record and finally with {@link Record#LAST}
     * @param cancel    atomic flag that can be set to true to stop consumption early
     */
    private void consume(String clusterId, Topic topic, ConsumeFilter filter, Consumer<Record> onRecord, AtomicBoolean cancel) {
        var partitionList = topic.getPartitions().stream()
                .filter(it -> (filter.partitions().isEmpty() || filter.partitions().contains(it.getId())) && it.getBeginOffset() < it.getEndOffset())
                .toList();

        if (partitionList.isEmpty()) {
            onRecord.accept(Record.LAST);
            return;
        }

        var partitionOffsetMap = new HashMap<TopicPartition, Long>();
        var partitionEndOffsetMap = partitionList.stream().collect(Collectors.toMap(Topic.Partition::getTp, Topic.Partition::getEndOffset));

        switch (filter.from().type()) {
            case BEGIN -> {
                partitionList.forEach(it -> partitionOffsetMap.put(it.getTp(), it.getBeginOffset()));
            }
            case END -> {
                var perPartitionSize = filter.maxResults() / partitionList.size();
                // todo add support for situation where data is unevenly distributed across partitions
                partitionList.forEach(it -> {
                    var offset = it.getEndOffset() - perPartitionSize;
                    partitionOffsetMap.put(it.getTp(), Math.max(offset, it.getBeginOffset()));
                });
            }
            case OFFSET -> {
                if (filter.from().offset().isEmpty()) throw new IllegalArgumentException();
                partitionList.stream()
                        .filter(it -> it.getBeginOffset() <= filter.from().offset().get() && it.getEndOffset() > filter.from().offset().get())
                        .forEach(it -> partitionOffsetMap.put(it.getTp(), filter.from().offset().get()));
            }
            case DATETIME, TIMESTAMP -> {
                if (filter.from().timestamp().isEmpty()) throw new IllegalArgumentException();
                try (var consumer = consumer(clusterId)) {
                    var partitionTimestampMap = partitionList.stream()
                            .collect(Collectors.toMap(Topic.Partition::getTp, it -> filter.from().timestamp().get()));
                    consumer.offsetsForTimes(partitionTimestampMap)
                            .forEach((k, v) -> {
                                if (v != null) partitionOffsetMap.put(k, v.offset());
                            });
                }
            }
        }

        if (partitionOffsetMap.isEmpty()) {
            onRecord.accept(Record.LAST);
            return;
        }

        var properties = new Properties();
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        try (var consumer = consumer(clusterId, properties)) {
            consumer.assign(partitionOffsetMap.keySet());
            partitionOffsetMap.forEach(consumer::seek);

            var expressionList = filter.expressions().stream()
                    .filter(ConsumeFilter.Expression::isActive)
                    .map(it -> StreamUtils.tryOrEmpty(() -> NASHORN_SCRIPT_ENGINE.compile("function() { " + it.code() + " }")))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();

            var recordCount = 0;
            var emptyPollCount = 0;
            var doPoll = true;
            var forcedStopPoll = 0; // in case make mistake in the doPoll condition (considering that max.poll.records=100 and filter.maxResults() <= 1000, there should be a maximum of 10 passes)
            while (doPoll && emptyPollCount < 3 && forcedStopPoll < 10 && !cancel.get()) {
                doPoll = false;
                forcedStopPoll++;

                var records = consumer.poll(Duration.ofMillis(1000));
                for (var record : records) {
                    if (recordCount < filter.maxResults()) {
                        var kv = deserializationService.deserialize(clusterId, topic, record, filter.keySerde(), filter.valueSerde());
                        var resultRecord = new Record(record, kv.getKey(), kv.getValue());
                        var match = true;
                        if (!expressionList.isEmpty()) {
                            match = false;
                            var bindings = NASHORN_SCRIPT_ENGINE.createBindings();
                            bindings.put("key", record.key() == null ? null : resultRecord.isKeyIsJson() ? deserializationService.tryToMap(resultRecord.getKey()) : resultRecord.getKey());
                            bindings.put("value", record.value() == null ? null : resultRecord.isValueIsJson() ? deserializationService.tryToMap(resultRecord.getValue()) : resultRecord.getValue());
                            bindings.put("headers", resultRecord.getHeaders());
                            bindings.put("offset", record.offset());
                            bindings.put("partition", record.partition());
                            bindings.put("timestamp", record.timestamp());
                            for (var expression : expressionList) {
                                try {
                                    match = match || (Boolean) ((ScriptObjectMirror) expression.eval(bindings)).call(null);
                                } catch (Exception e) {
                                    logDebugError(e);
                                }
                            }
                        }
                        if (match) {
                            onRecord.accept(resultRecord);
                            recordCount++;
                        }
                    }
                }

                if (recordCount < filter.maxResults()) {
                    for (var tp : consumer.assignment()) {
                        if (partitionEndOffsetMap.containsKey(tp) && partitionEndOffsetMap.get(tp) > consumer.position(tp)) {
                            doPoll = true;
                            break;
                        }
                    }
                    if (records.isEmpty()) {
                        emptyPollCount++;
                        doPoll = true;
                    }
                }
            }
        }
        onRecord.accept(Record.LAST);
    }

    public CompletableFuture<Void> tryCompileExpression(String code) {
        return CompletableFuture.runAsync(() -> StreamUtils.tryReturn(() -> NASHORN_SCRIPT_ENGINE.compile("function() { " + code + " }")));
    }

    public CompletableFuture<Record> produce(String clusterId, String topicName, NewRecord record) {
        return CompletableFuture.supplyAsync(() -> {
            var properties = new Properties();
            properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, record.getCompression().getValue());
            properties.put(ProducerConfig.ACKS_CONFIG, record.getAsks().getValue());
            properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, record.isIdempotence());
            try (var producer = producer(clusterId, properties)) {
                var producerRecord = serializationService.serialize(clusterId, topicName, record);
                var result = new CompletableFuture<Record>();
                producer.send(producerRecord, (metadata, exception) -> {
                    if (exception != null) {
                        result.completeExceptionally(exception);
                    } else {
                        result.complete(new Record(record, metadata));
                    }
                });
                return result;
            }
        }).thenCompose(Function.identity());
    }
}
