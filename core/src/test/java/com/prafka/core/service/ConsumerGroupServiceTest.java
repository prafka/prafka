package com.prafka.core.service;

import com.prafka.core.model.ConsumerGroup;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConsumerGroupServiceTest {

    private AdminClient adminClient = mock(AdminClient.class);
    private Consumer<byte[], byte[]> consumer = mock(Consumer.class);
    private ConsumerGroupService consumerGroupService = new ConsumerGroupService() {
        @Override
        protected AdminClient adminClient(String clusterId) {
            return adminClient;
        }
        @Override
        protected Consumer<byte[], byte[]> consumer(String clusterId) {
            return consumer;
        }
        @Override
        protected Consumer<byte[], byte[]> consumer(String clusterId, Properties properties) {
            return consumer;
        }
    };

    @Test
    void shouldGetAll() throws Exception {
        // Given
        var clusterId = "test-cluster";

        var mockListConsumerGroupsResult = mock(ListConsumerGroupsResult.class);
        var mockDescribeConsumerGroupsResult = mock(DescribeConsumerGroupsResult.class);
        var mockConsumerGroupDescription = new ConsumerGroupDescription("group1", true,
                Collections.emptyList(), "1", GroupType.CONSUMER, GroupState.STABLE, mock(Node.class),
                Collections.emptySet(), Optional.empty(), Optional.empty());
        var mockListConsumerGroupOffsetsResult = mock(ListConsumerGroupOffsetsResult.class);

        var topicPartition = new TopicPartition("topic1", 0);
        var offsetAndMetadata = new OffsetAndMetadata(10L);
        var offsetMap = Map.of(topicPartition, offsetAndMetadata);
        var beginOffsetMap = Map.of(topicPartition, 0L);
        var endOffsetMap = Map.of(topicPartition, 20L);

        when(mockListConsumerGroupsResult.all()).thenReturn(KafkaFuture.completedFuture(List.of(new ConsumerGroupListing("group1", true))));
        when(adminClient.listConsumerGroups()).thenReturn(mockListConsumerGroupsResult);
        when(mockDescribeConsumerGroupsResult.describedGroups()).thenReturn(Map.of("group1", KafkaFuture.completedFuture(mockConsumerGroupDescription)));
        when(adminClient.describeConsumerGroups(anyList())).thenReturn(mockDescribeConsumerGroupsResult);
        when(adminClient.listConsumerGroupOffsets(anyMap())).thenReturn(mockListConsumerGroupOffsetsResult);
        when(mockListConsumerGroupOffsetsResult.partitionsToOffsetAndMetadata(anyString())).thenReturn(KafkaFuture.completedFuture(offsetMap));
        when(consumer.beginningOffsets(anyCollection())).thenReturn(beginOffsetMap);
        when(consumer.endOffsets(anyCollection())).thenReturn(endOffsetMap);

        // When
        var result = consumerGroupService.getAll(clusterId);

        // Then
        var consumerGroupList = new ArrayList<>(result.get());
        assertEquals(1, consumerGroupList.size());
        assertEquals("group1", consumerGroupList.getFirst().getId());
    }

    @Test
    void shouldCreate() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var groupId = "test-group";
        var strategy = ConsumerGroup.OffsetStrategy.EARLIEST;
        var topics = List.of("topic1", "topic2");

        var partition1 = new TopicPartition("topic1", 0);
        var partition2 = new TopicPartition("topic2", 0);
        var topicPartitions = List.of(partition1, partition2);

        when(consumer.partitionsFor("topic1")).thenReturn(List.of(new PartitionInfo("topic1", 0, null, null, null)));
        when(consumer.partitionsFor("topic2")).thenReturn(List.of(new PartitionInfo("topic2", 0, null, null, null)));
        when(consumer.beginningOffsets(topicPartitions)).thenReturn(Map.of(partition1, 0L, partition2, 0L));

        // When
        consumerGroupService.create(clusterId, groupId, strategy, topics).get();

        // Then
        verify(consumer).assign(topicPartitions);
        verify(consumer).commitSync(anyMap());
    }

    @Test
    void shouldCalculateNewOffsetsForEarliestStrategy() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var groupId = "test-group";

        var topicPartition = new TopicPartition("topic1", 0);
        var offset = new ConsumerGroup.Offset(100L, 50L, 200L);
        var offsets = Map.of(topicPartition, offset);

        var filter = new ConsumerGroupService.CalculateNewOffsetsFilter(
                ConsumerGroup.OffsetStrategy.EARLIEST,
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );

        // When
        var result = consumerGroupService.calculateNewOffsets(clusterId, groupId, offsets, filter).get();

        // Then
        assertEquals(1, result.size());
        assertEquals(50L, result.get(topicPartition));
    }

    @Test
    void shouldCalculateNewOffsetsForLatestStrategy() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var groupId = "test-group";

        var topicPartition = new TopicPartition("topic1", 0);
        var offset = new ConsumerGroup.Offset(100L, 50L, 200L);
        var offsets = Map.of(topicPartition, offset);

        var filter = new ConsumerGroupService.CalculateNewOffsetsFilter(
                ConsumerGroup.OffsetStrategy.LATEST,
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );

        // When
        var result = consumerGroupService.calculateNewOffsets(clusterId, groupId, offsets, filter).get();

        // Then
        assertEquals(1, result.size());
        assertEquals(200L, result.get(topicPartition));
    }

    @Test
    void shouldCalculateNewOffsetsForSpecificStrategyWithinRange() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var groupId = "test-group";

        var topicPartition = new TopicPartition("topic1", 0);
        var offset = new ConsumerGroup.Offset(100L, 50L, 200L);
        var offsets = Map.of(topicPartition, offset);

        var filter = new ConsumerGroupService.CalculateNewOffsetsFilter(
                ConsumerGroup.OffsetStrategy.SPECIFIC,
                Optional.of(150),
                Optional.empty(),
                Optional.empty()
        );

        // When
        var result = consumerGroupService.calculateNewOffsets(clusterId, groupId, offsets, filter).get();

        // Then
        assertEquals(1, result.size());
        assertEquals(150L, result.get(topicPartition));
    }

    @Test
    void shouldCalculateNewOffsetsForSpecificStrategyOutsideRange() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var groupId = "test-group";

        var topicPartition = new TopicPartition("topic1", 0);
        var offset = new ConsumerGroup.Offset(100L, 50L, 200L);
        var offsets = Map.of(topicPartition, offset);

        var filter = new ConsumerGroupService.CalculateNewOffsetsFilter(
                ConsumerGroup.OffsetStrategy.SPECIFIC,
                Optional.of(300),
                Optional.empty(),
                Optional.empty()
        );

        // When
        var result = consumerGroupService.calculateNewOffsets(clusterId, groupId, offsets, filter).get();

        // Then
        assertEquals(1, result.size());
        assertEquals(100L, result.get(topicPartition));
    }

    @Test
    void shouldCalculateNewOffsetsForShiftStrategy() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var groupId = "test-group";

        var topicPartition = new TopicPartition("topic1", 0);
        var offset = new ConsumerGroup.Offset(100L, 50L, 200L);
        var offsets = Map.of(topicPartition, offset);

        var filter = new ConsumerGroupService.CalculateNewOffsetsFilter(
                ConsumerGroup.OffsetStrategy.SHIFT,
                Optional.empty(),
                Optional.of(25),
                Optional.empty()
        );

        // When
        var result = consumerGroupService.calculateNewOffsets(clusterId, groupId, offsets, filter).get();

        // Then
        assertEquals(1, result.size());
        assertEquals(125L, result.get(topicPartition));
    }

    @Test
    void shouldCalculateNewOffsetsForShiftStrategyNotExceedingEnd() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var groupId = "test-group";

        var topicPartition = new TopicPartition("topic1", 0);
        var offset = new ConsumerGroup.Offset(190L, 50L, 200L);
        var offsets = Map.of(topicPartition, offset);

        var filter = new ConsumerGroupService.CalculateNewOffsetsFilter(
                ConsumerGroup.OffsetStrategy.SHIFT,
                Optional.empty(),
                Optional.of(25),
                Optional.empty()
        );

        // When
        var result = consumerGroupService.calculateNewOffsets(clusterId, groupId, offsets, filter).get();

        // Then
        assertEquals(1, result.size());
        assertEquals(200L, result.get(topicPartition));
    }

    @Test
    void shouldCalculateNewOffsetsForTimestampStrategy() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var groupId = "test-group";

        var topicPartition = new TopicPartition("topic1", 0);
        var offset = new ConsumerGroup.Offset(100L, 50L, 200L);
        var offsets = Map.of(topicPartition, offset);

        var timestamp = 1234567890L;
        var filter = new ConsumerGroupService.CalculateNewOffsetsFilter(
                ConsumerGroup.OffsetStrategy.TIMESTAMP,
                Optional.empty(),
                Optional.empty(),
                Optional.of(timestamp)
        );

        var offsetAndTimestamp = new OffsetAndTimestamp(75L, timestamp);
        var offsetForTimesResult = Map.of(topicPartition, offsetAndTimestamp);

        when(consumer.offsetsForTimes(anyMap())).thenReturn(offsetForTimesResult);

        // When
        var result = consumerGroupService.calculateNewOffsets(clusterId, groupId, offsets, filter).get();

        // Then
        assertEquals(1, result.size());
        assertEquals(75L, result.get(topicPartition));
    }

    @Test
    void shouldCalculateNewOffsetsForDateTimeStrategy() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var groupId = "test-group";

        var topicPartition = new TopicPartition("topic1", 0);
        var offset = new ConsumerGroup.Offset(100L, 50L, 200L);
        var offsets = Map.of(topicPartition, offset);

        var timestamp = 1234567890L;
        var filter = new ConsumerGroupService.CalculateNewOffsetsFilter(
                ConsumerGroup.OffsetStrategy.DATETIME,
                Optional.empty(),
                Optional.empty(),
                Optional.of(timestamp)
        );

        var offsetAndTimestamp = new OffsetAndTimestamp(85L, timestamp);
        var offsetForTimesResult = Map.of(topicPartition, offsetAndTimestamp);

        when(consumer.offsetsForTimes(anyMap())).thenReturn(offsetForTimesResult);

        // When
        var result = consumerGroupService.calculateNewOffsets(clusterId, groupId, offsets, filter).get();

        // Then
        assertEquals(1, result.size());
        assertEquals(85L, result.get(topicPartition));
    }

    @Test
    void shouldGetAllGroupsSummary() throws Exception {
        // Given
        var clusterId = "test-cluster";

        var mockListConsumerGroupsResult = mock(ListConsumerGroupsResult.class);
        var groupListings = List.of(
                new ConsumerGroupListing("group1", Optional.of(GroupState.STABLE), Optional.of(GroupType.CONSUMER), true),
                new ConsumerGroupListing("group2", Optional.of(GroupState.PREPARING_REBALANCE), Optional.of(GroupType.CONSUMER), true),
                new ConsumerGroupListing("group3", Optional.of(GroupState.COMPLETING_REBALANCE), Optional.of(GroupType.CONSUMER), true),
                new ConsumerGroupListing("group4", Optional.of(GroupState.EMPTY), Optional.of(GroupType.CONSUMER), true),
                new ConsumerGroupListing("group5", Optional.of(GroupState.DEAD), Optional.of(GroupType.CONSUMER), true)
        );

        when(mockListConsumerGroupsResult.all()).thenReturn(KafkaFuture.completedFuture(groupListings));
        when(adminClient.listConsumerGroups()).thenReturn(mockListConsumerGroupsResult);

        // When
        var result = consumerGroupService.getAllGroupsSummary(clusterId).get();

        // Then
        assertEquals(5, result.groupCount());
        assertEquals(1, result.stableCount());
        assertEquals(1, result.preparingRebalanceCount());
        assertEquals(1, result.completingRebalanceCount());
        assertEquals(1, result.emptyCount());
        assertEquals(1, result.deadCount());
    }
}