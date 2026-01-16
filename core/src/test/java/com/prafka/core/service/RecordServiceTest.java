package com.prafka.core.service;

import com.prafka.core.model.*;
import com.prafka.core.model.Record;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecordServiceTest {

    private TopicService topicService = mock(TopicService.class);
    private RecordSerializationService serializationService = mock(RecordSerializationService.class);
    private RecordDeserializationService deserializationService = mock(RecordDeserializationService.class);
    private Consumer<byte[], byte[]> consumer = mock(Consumer.class);
    private Producer<byte[], byte[]> producer = mock(Producer.class);
    private RecordService recordService = new RecordService(topicService, serializationService, deserializationService) {
        @Override
        protected Consumer<byte[], byte[]> consumer(String clusterId) {
            return consumer;
        }
        @Override
        protected Consumer<byte[], byte[]> consumer(String clusterId, Properties properties) {
            return consumer;
        }
        @Override
        protected Producer<byte[], byte[]> producer(String clusterId, Properties properties) {
            return producer;
        }
    };

    @Test
    void shouldConsumeLastRecordWhenPartitionListIsEmpty() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var queue = new ArrayBlockingQueue<Record>(1);
        var cancel = new AtomicBoolean(false);
        var filter = mock(ConsumeFilter.class);

        var topic = mock(Topic.class);
        when(topic.getPartitions()).thenReturn(new ArrayList<>());
        when(filter.partitions()).thenReturn(new ArrayList<>());

        when(topicService.get(any(), any())).thenReturn(CompletableFuture.completedFuture(topic));

        // When
        recordService.consume(clusterId, "topic-name", filter, queue, cancel).get();

        // Then
        assertEquals(1, queue.size());
        assertEquals(Record.LAST, queue.poll());
    }

    @Test
    void shouldConsumeLastRecordWhenPartitionOffsetMapIsEmpty() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var queue = new ArrayBlockingQueue<Record>(1);
        var cancel = new AtomicBoolean(false);
        var topic = mock(Topic.class);
        var filter = mock(ConsumeFilter.class);
        var from = mock(ConsumeFilter.From.class);
        var partition = mock(Topic.Partition.class);
        var topicPartition = new TopicPartition("test-topic", 0);

        when(topic.getPartitions()).thenReturn(List.of(partition));
        when(filter.partitions()).thenReturn(new ArrayList<>());
        when(partition.getId()).thenReturn(0);
        when(partition.getBeginOffset()).thenReturn(100L);
        when(partition.getEndOffset()).thenReturn(100L);
        when(partition.getTp()).thenReturn(topicPartition);

        when(filter.from()).thenReturn(from);
        when(from.type()).thenReturn(ConsumeFilter.From.Type.BEGIN);

        when(topicService.get(any(), any())).thenReturn(CompletableFuture.completedFuture(topic));

        // When
        recordService.consume(clusterId, "topic-name", filter, queue, cancel).get();

        // Then
        assertEquals(1, queue.size());
        assertEquals(Record.LAST, queue.poll());
    }

    @Test
    void shouldConsumeByBeginType() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var queue = new ArrayBlockingQueue<Record>(1);
        var cancel = new AtomicBoolean(false);
        var topic = mock(Topic.class);
        var filter = mock(ConsumeFilter.class);
        var from = mock(ConsumeFilter.From.class);

        var partition = mock(Topic.Partition.class);
        var topicPartition = new TopicPartition("test-topic", 0);

        when(topic.getPartitions()).thenReturn(List.of(partition));
        when(filter.partitions()).thenReturn(new ArrayList<>());
        when(filter.maxResults()).thenReturn(100);
        when(filter.expressions()).thenReturn(new ArrayList<>());
        when(filter.keySerde()).thenReturn(SerdeType.STRING);
        when(filter.valueSerde()).thenReturn(SerdeType.STRING);

        when(partition.getId()).thenReturn(0);
        when(partition.getBeginOffset()).thenReturn(0L);
        when(partition.getEndOffset()).thenReturn(200L);
        when(partition.getTp()).thenReturn(topicPartition);

        when(filter.from()).thenReturn(from);
        when(from.type()).thenReturn(ConsumeFilter.From.Type.BEGIN);

        when(topicService.get(any(), any())).thenReturn(CompletableFuture.completedFuture(topic));
        when(consumer.poll(any())).thenReturn(new ConsumerRecords<>(new HashMap<>(), new HashMap<>()));

        // When
        recordService.consume(clusterId, "topic-name", filter, queue, cancel).get();

        // Then
        assertEquals(1, queue.size());
        verify(consumer).assign(Set.of(topicPartition));
        verify(consumer).seek(topicPartition, 0L);
    }

    @Test
    void shouldConsumeByEndType() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var queue = new ArrayBlockingQueue<Record>(1);
        var cancel = new AtomicBoolean(false);
        var topic = mock(Topic.class);
        var filter = mock(ConsumeFilter.class);
        var from = mock(ConsumeFilter.From.class);

        var partition = mock(Topic.Partition.class);
        var topicPartition = new TopicPartition("test-topic", 0);

        when(topic.getPartitions()).thenReturn(List.of(partition));
        when(filter.partitions()).thenReturn(new ArrayList<>());
        when(filter.maxResults()).thenReturn(100);
        when(filter.expressions()).thenReturn(new ArrayList<>());
        when(filter.keySerde()).thenReturn(SerdeType.STRING);
        when(filter.valueSerde()).thenReturn(SerdeType.STRING);

        when(partition.getId()).thenReturn(0);
        when(partition.getBeginOffset()).thenReturn(0L);
        when(partition.getEndOffset()).thenReturn(200L);
        when(partition.getTp()).thenReturn(topicPartition);

        when(filter.from()).thenReturn(from);
        when(from.type()).thenReturn(ConsumeFilter.From.Type.END);

        when(topicService.get(any(), any())).thenReturn(CompletableFuture.completedFuture(topic));
        when(consumer.poll(any())).thenReturn(new ConsumerRecords<>(new HashMap<>(), new HashMap<>()));

        // When
        recordService.consume(clusterId, "topic-name", filter, queue, cancel).get();

        // Then
        assertEquals(1, queue.size());
        verify(consumer).assign(Set.of(topicPartition));
        verify(consumer).seek(topicPartition, 100L);
    }

    @Test
    void shouldConsumeByOffsetType() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var queue = new ArrayBlockingQueue<Record>(1);
        var cancel = new AtomicBoolean(false);
        var topic = mock(Topic.class);
        var filter = mock(ConsumeFilter.class);
        var from = mock(ConsumeFilter.From.class);

        var partition = mock(Topic.Partition.class);
        var topicPartition = new TopicPartition("test-topic", 0);

        when(topic.getPartitions()).thenReturn(List.of(partition));
        when(filter.partitions()).thenReturn(new ArrayList<>());
        when(filter.maxResults()).thenReturn(100);
        when(filter.expressions()).thenReturn(new ArrayList<>());
        when(filter.keySerde()).thenReturn(SerdeType.STRING);
        when(filter.valueSerde()).thenReturn(SerdeType.STRING);

        when(partition.getId()).thenReturn(0);
        when(partition.getBeginOffset()).thenReturn(0L);
        when(partition.getEndOffset()).thenReturn(100L);
        when(partition.getTp()).thenReturn(topicPartition);

        when(filter.from()).thenReturn(from);
        when(from.type()).thenReturn(ConsumeFilter.From.Type.OFFSET);
        when(from.offset()).thenReturn(Optional.of(50L));

        when(topicService.get(any(), any())).thenReturn(CompletableFuture.completedFuture(topic));
        when(consumer.poll(any())).thenReturn(new ConsumerRecords<>(new HashMap<>(), new HashMap<>()));

        // When
        recordService.consume(clusterId, "topic-name", filter, queue, cancel).get();

        // Then
        assertEquals(1, queue.size());
        verify(consumer).assign(Set.of(topicPartition));
        verify(consumer).seek(topicPartition, 50L);
    }

    @Test
    void shouldConsumeByTimestampType() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var queue = new ArrayBlockingQueue<Record>(1);
        var cancel = new AtomicBoolean(false);
        var topic = mock(Topic.class);
        var filter = mock(ConsumeFilter.class);
        var from = mock(ConsumeFilter.From.class);

        var partition = mock(Topic.Partition.class);
        var topicPartition = new TopicPartition("test-topic", 0);

        when(topic.getPartitions()).thenReturn(List.of(partition));
        when(filter.partitions()).thenReturn(new ArrayList<>());
        when(filter.maxResults()).thenReturn(100);
        when(filter.expressions()).thenReturn(new ArrayList<>());
        when(filter.keySerde()).thenReturn(SerdeType.STRING);
        when(filter.valueSerde()).thenReturn(SerdeType.STRING);

        when(partition.getId()).thenReturn(0);
        when(partition.getBeginOffset()).thenReturn(0L);
        when(partition.getEndOffset()).thenReturn(100L);
        when(partition.getTp()).thenReturn(topicPartition);

        when(filter.from()).thenReturn(from);
        when(from.type()).thenReturn(ConsumeFilter.From.Type.TIMESTAMP);
        when(from.timestamp()).thenReturn(Optional.of(1000L));

        var offsetMap = new HashMap<TopicPartition, OffsetAndTimestamp>();
        offsetMap.put(topicPartition, new OffsetAndTimestamp(25L, 1000L));

        when(topicService.get(any(), any())).thenReturn(CompletableFuture.completedFuture(topic));
        when(consumer.offsetsForTimes(anyMap())).thenReturn(offsetMap);
        when(consumer.poll(any())).thenReturn(new ConsumerRecords<>(new HashMap<>(), new HashMap<>()));

        // When
        recordService.consume(clusterId, "topic-name", filter, queue, cancel).get();

        // Then
        assertEquals(1, queue.size());
        verify(consumer).assign(Set.of(topicPartition));
        verify(consumer).seek(topicPartition, 25L);
    }

    @Test
    void shouldConsume() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var queue = new ArrayBlockingQueue<Record>(3);
        var cancel = new AtomicBoolean(false);
        var topic = mock(Topic.class);
        var filter = mock(ConsumeFilter.class);
        var from = mock(ConsumeFilter.From.class);

        var partition = mock(Topic.Partition.class);
        var topicPartition = new TopicPartition("test-topic", 0);

        when(topic.getPartitions()).thenReturn(List.of(partition));
        when(filter.partitions()).thenReturn(new ArrayList<>());
        when(filter.maxResults()).thenReturn(100);
        when(filter.expressions()).thenReturn(new ArrayList<>());
        when(filter.keySerde()).thenReturn(SerdeType.STRING);
        when(filter.valueSerde()).thenReturn(SerdeType.STRING);

        when(partition.getId()).thenReturn(0);
        when(partition.getBeginOffset()).thenReturn(0L);
        when(partition.getEndOffset()).thenReturn(100L);
        when(partition.getTp()).thenReturn(topicPartition);

        when(filter.from()).thenReturn(from);
        when(from.type()).thenReturn(ConsumeFilter.From.Type.BEGIN);

        var recordsMap = new HashMap<TopicPartition, List<ConsumerRecord<byte[], byte[]>>>();
        var kafkaRecords = new ArrayList<ConsumerRecord<byte[], byte[]>>();

        for (int i = 0; i < 2; i++) {
            kafkaRecords.add(new ConsumerRecord<>("test-topic", 0, i, ("key" + i).getBytes(), ("value" + i).getBytes()));
        }

        recordsMap.put(topicPartition, kafkaRecords);
        var consumerRecords = new ConsumerRecords<>(recordsMap, new HashMap<>());

        when(deserializationService.deserialize(anyString(), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    ConsumerRecord<byte[], byte[]> record = invocation.getArgument(2);
                    return Pair.of(new String(record.key()), new String(record.value()));
                });

        when(topicService.get(any(), any())).thenReturn(CompletableFuture.completedFuture(topic));
        when(consumer.poll(any())).thenReturn(consumerRecords).thenReturn(new ConsumerRecords<>(new HashMap<>(), new HashMap<>()));

        // When
        recordService.consume(clusterId, "topic-name", filter, queue, cancel).get();

        // Then
        assertEquals(3, queue.size());
        assertEquals("key0", queue.poll().getKey());
        assertEquals("key1", queue.poll().getKey());
        assertEquals(Record.LAST, queue.poll());
    }

    @Test
    void shouldConsumeByExpression() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var queue = new ArrayBlockingQueue<Record>(3);
        var cancel = new AtomicBoolean(false);
        var topic = mock(Topic.class);
        var filter = mock(ConsumeFilter.class);
        var from = mock(ConsumeFilter.From.class);

        var partition = mock(Topic.Partition.class);
        var topicPartition = new TopicPartition("test-topic", 0);

        when(topic.getPartitions()).thenReturn(List.of(partition));
        when(filter.partitions()).thenReturn(new ArrayList<>());
        when(filter.maxResults()).thenReturn(100);
        when(filter.expressions()).thenReturn(new ArrayList<>());
        when(filter.keySerde()).thenReturn(SerdeType.STRING);
        when(filter.valueSerde()).thenReturn(SerdeType.STRING);

        var expression = mock(ConsumeFilter.Expression.class);
        when(expression.isActive()).thenReturn(true);
        when(expression.code()).thenReturn("return key === 'key1';");
        when(filter.expressions()).thenReturn(List.of(expression));

        when(partition.getId()).thenReturn(0);
        when(partition.getBeginOffset()).thenReturn(0L);
        when(partition.getEndOffset()).thenReturn(100L);
        when(partition.getTp()).thenReturn(topicPartition);

        when(filter.from()).thenReturn(from);
        when(from.type()).thenReturn(ConsumeFilter.From.Type.BEGIN);

        var recordsMap = new HashMap<TopicPartition, List<ConsumerRecord<byte[], byte[]>>>();
        var kafkaRecords = new ArrayList<ConsumerRecord<byte[], byte[]>>();

        for (int i = 0; i < 2; i++) {
            kafkaRecords.add(new ConsumerRecord<>("test-topic", 0, i, ("key" + i).getBytes(), ("value" + i).getBytes()));
        }

        recordsMap.put(topicPartition, kafkaRecords);
        var consumerRecords = new ConsumerRecords<>(recordsMap, new HashMap<>());

        when(deserializationService.deserialize(anyString(), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    ConsumerRecord<byte[], byte[]> record = invocation.getArgument(2);
                    return Pair.of(new String(record.key()), new String(record.value()));
                });

        when(topicService.get(any(), any())).thenReturn(CompletableFuture.completedFuture(topic));
        when(consumer.poll(any())).thenReturn(consumerRecords).thenReturn(new ConsumerRecords<>(new HashMap<>(), new HashMap<>()));

        // When
        recordService.consume(clusterId, "topic-name", filter, queue, cancel).get();

        // Then
        assertEquals(2, queue.size());
        assertEquals("key1", queue.poll().getKey());
        assertEquals(Record.LAST, queue.poll());
    }

    @Test
    void shouldProduce() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var topicName = "test-topic";
        var producerRecord = mock(ProducerRecord.class);
        var recordMetadata = mock(RecordMetadata.class);
        var newRecord = new NewRecord();
        newRecord.setKey("test-key");
        newRecord.setValue("test-value");

        when(serializationService.serialize(clusterId, topicName, newRecord)).thenReturn(producerRecord);

        // When
        var result = recordService.produce(clusterId, topicName, newRecord);

        // Simulate the callback being called with successful result
        Thread.sleep(50);
        var callbackCaptor = ArgumentCaptor.forClass(Callback.class);
        verify(producer).send(any(), callbackCaptor.capture());
        callbackCaptor.getValue().onCompletion(recordMetadata, null);

        // Then
        assertEquals(newRecord.getKey(), result.get().getKey());
    }

}