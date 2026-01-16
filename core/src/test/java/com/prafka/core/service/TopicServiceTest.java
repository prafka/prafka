package com.prafka.core.service;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
}