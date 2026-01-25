package com.prafka.core.service;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class TopicServiceTest {

    private AdminClient adminClient = mock(AdminClient.class);
    private Consumer<byte[], byte[]> consumer = mock(Consumer.class);
    private TopicService topicService = new TopicService() {
        @Override
        protected AdminClient adminClient(String clusterId) {
            return adminClient;
        }
        @Override
        protected Consumer<byte[], byte[]> consumer(String clusterId) {
            return consumer;
        }
    };

    @Test
    void shouldGetAll() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var topicNames = List.of("topic1", "topic2");

        var topicDescriptions = List.of(
                new TopicDescription("topic1", false, List.of(
                        new TopicPartitionInfo(0, null, Collections.emptyList(), Collections.emptyList()),
                        new TopicPartitionInfo(1, null, Collections.emptyList(), Collections.emptyList())
                ), Collections.emptySet()),
                new TopicDescription("topic2", false, List.of(
                        new TopicPartitionInfo(0, null, Collections.emptyList(), Collections.emptyList())
                ), Collections.emptySet())
        );

        var descriptionFutures = topicDescriptions.stream()
                .map(KafkaFuture::completedFuture)
                .toList();

        var mockDescribeResult = mock(DescribeTopicsResult.class);
        when(mockDescribeResult.topicNameValues()).thenReturn(
                topicNames.stream().collect(Collectors.toMap(
                        name -> name,
                        name -> descriptionFutures.get(topicNames.indexOf(name))
                ))
        );

        when(adminClient.describeTopics(anyCollection())).thenReturn(mockDescribeResult);

        var beginOffsets = new HashMap<TopicPartition, Long>();
        var endOffsets = new HashMap<TopicPartition, Long>();

        beginOffsets.put(new TopicPartition("topic1", 0), 0L);
        beginOffsets.put(new TopicPartition("topic1", 1), 0L);
        beginOffsets.put(new TopicPartition("topic2", 0), 0L);

        endOffsets.put(new TopicPartition("topic1", 0), 100L);
        endOffsets.put(new TopicPartition("topic1", 1), 200L);
        endOffsets.put(new TopicPartition("topic2", 0), 150L);

        when(consumer.beginningOffsets(any())).thenReturn(beginOffsets);
        when(consumer.endOffsets(any())).thenReturn(endOffsets);

        // When
        var result = topicService.getAll(clusterId, topicNames).get();

        assertEquals(2, result.size());
        assertTrue(result.containsKey("topic1"));
        assertTrue(result.containsKey("topic2"));

        var topic1 = result.get("topic1");
        assertEquals("topic1", topic1.getName());
        assertEquals(2, topic1.getPartitions().size());

        var topic2 = result.get("topic2");
        assertEquals("topic2", topic2.getName());
        assertEquals(1, topic2.getPartitions().size());
    }

    @Test
    void shouldGetAllTopicNames() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var mockListTopicsResult = mock(ListTopicsResult.class);
        var topicNames = Set.of("topic1", "topic2", "topic3");

        when(mockListTopicsResult.names()).thenReturn(KafkaFuture.completedFuture(topicNames));
        when(adminClient.listTopics(any(ListTopicsOptions.class))).thenReturn(mockListTopicsResult);

        // When
        var result = topicService.getAllNames(clusterId).get();

        // Then
        assertEquals(3, result.size());
        assertTrue(result.contains("topic1"));
        assertTrue(result.contains("topic2"));
        assertTrue(result.contains("topic3"));
    }

    @Test
    void shouldCreateTopic() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var topicName = "new-topic";
        var partitions = 3;
        short replicationFactor = 2;
        var configs = Map.of("retention.ms", "86400000");

        var mockCreateTopicsResult = mock(CreateTopicsResult.class);
        when(mockCreateTopicsResult.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(adminClient.createTopics(anyCollection())).thenReturn(mockCreateTopicsResult);

        var topicDescription = new TopicDescription(topicName, false, List.of(
                new TopicPartitionInfo(0, null, Collections.emptyList(), Collections.emptyList()),
                new TopicPartitionInfo(1, null, Collections.emptyList(), Collections.emptyList()),
                new TopicPartitionInfo(2, null, Collections.emptyList(), Collections.emptyList())
        ), Collections.emptySet());

        var mockDescribeResult = mock(DescribeTopicsResult.class);
        when(mockDescribeResult.topicNameValues()).thenReturn(
                Map.of(topicName, KafkaFuture.completedFuture(topicDescription))
        );
        when(adminClient.describeTopics(anyCollection())).thenReturn(mockDescribeResult);

        var beginOffsets = new HashMap<TopicPartition, Long>();
        var endOffsets = new HashMap<TopicPartition, Long>();
        for (int i = 0; i < partitions; i++) {
            beginOffsets.put(new TopicPartition(topicName, i), 0L);
            endOffsets.put(new TopicPartition(topicName, i), 0L);
        }
        when(consumer.beginningOffsets(any())).thenReturn(beginOffsets);
        when(consumer.endOffsets(any())).thenReturn(endOffsets);

        // When
        var result = topicService.create(clusterId, topicName, partitions, replicationFactor, configs).get();

        // Then
        assertEquals(topicName, result.getName());
        assertEquals(partitions, result.getPartitions().size());
        verify(adminClient).createTopics(anyCollection());
    }

    @Test
    void shouldDeleteTopic() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var topicName = "topic-to-delete";

        var mockDeleteTopicsResult = mock(DeleteTopicsResult.class);
        when(mockDeleteTopicsResult.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(adminClient.deleteTopics(anyCollection())).thenReturn(mockDeleteTopicsResult);

        // When
        topicService.delete(clusterId, topicName).get();

        // Then
        verify(adminClient).deleteTopics(List.of(topicName));
    }

    @Test
    void shouldDeleteMultipleTopics() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var topicNames = List.of("topic1", "topic2");

        var mockDeleteTopicsResult = mock(DeleteTopicsResult.class);
        when(mockDeleteTopicsResult.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(adminClient.deleteTopics(anyCollection())).thenReturn(mockDeleteTopicsResult);

        // When
        topicService.delete(clusterId, topicNames).get();

        // Then
        verify(adminClient).deleteTopics(topicNames);
    }

    @Test
    void shouldEmptyTopic() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var topicName = "topic-to-empty";

        var topicDescription = new TopicDescription(topicName, false, List.of(
                new TopicPartitionInfo(0, null, Collections.emptyList(), Collections.emptyList())
        ), Collections.emptySet());

        var mockDescribeResult = mock(DescribeTopicsResult.class);
        when(mockDescribeResult.topicNameValues()).thenReturn(
                Map.of(topicName, KafkaFuture.completedFuture(topicDescription))
        );
        when(adminClient.describeTopics(anyCollection())).thenReturn(mockDescribeResult);

        var tp = new TopicPartition(topicName, 0);
        when(consumer.beginningOffsets(any())).thenReturn(Map.of(tp, 0L));
        when(consumer.endOffsets(any())).thenReturn(Map.of(tp, 100L));

        var mockDeleteRecordsResult = mock(DeleteRecordsResult.class);
        when(mockDeleteRecordsResult.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(adminClient.deleteRecords(anyMap())).thenReturn(mockDeleteRecordsResult);

        // When
        topicService.empty(clusterId, topicName).get();

        // Then
        verify(adminClient).deleteRecords(anyMap());
    }

    @Test
    void shouldGetSingleTopic() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var topicName = "my-topic";

        var topicDescription = new TopicDescription(topicName, false, List.of(
                new TopicPartitionInfo(0, null, Collections.emptyList(), Collections.emptyList())
        ), Collections.emptySet());

        var mockDescribeResult = mock(DescribeTopicsResult.class);
        when(mockDescribeResult.topicNameValues()).thenReturn(
                Map.of(topicName, KafkaFuture.completedFuture(topicDescription))
        );
        when(adminClient.describeTopics(anyCollection())).thenReturn(mockDescribeResult);

        var tp = new TopicPartition(topicName, 0);
        when(consumer.beginningOffsets(any())).thenReturn(Map.of(tp, 0L));
        when(consumer.endOffsets(any())).thenReturn(Map.of(tp, 50L));

        // When
        var result = topicService.get(clusterId, topicName).get();

        // Then
        assertEquals(topicName, result.getName());
        assertEquals(1, result.getPartitions().size());
        assertEquals(50L, result.getRecordCount());
    }
}