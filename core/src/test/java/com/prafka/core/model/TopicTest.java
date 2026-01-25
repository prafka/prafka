package com.prafka.core.model;

import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.Uuid;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TopicTest {

    @Test
    void shouldCalculateRecordCount() {
        var node = new Node(1, "localhost", 9092);
        var topicDescription = new TopicDescription("test-topic", false, List.of(
                new TopicPartitionInfo(0, node, List.of(node), List.of(node)),
                new TopicPartitionInfo(1, node, List.of(node), List.of(node))
        ), Collections.emptySet(), Uuid.randomUuid());

        var beginOffsets = Map.of(
                new TopicPartition("test-topic", 0), 0L,
                new TopicPartition("test-topic", 1), 10L
        );
        var endOffsets = Map.of(
                new TopicPartition("test-topic", 0), 100L,
                new TopicPartition("test-topic", 1), 110L
        );

        var topic = new Topic(topicDescription, beginOffsets, endOffsets);

        assertEquals(200L, topic.getRecordCount());
    }

    @Test
    void shouldCalculateRecordCountForEmptyTopic() {
        var node = new Node(1, "localhost", 9092);
        var topicDescription = new TopicDescription("empty-topic", false, List.of(
                new TopicPartitionInfo(0, node, List.of(node), List.of(node))
        ), Collections.emptySet(), Uuid.randomUuid());

        var beginOffsets = Map.of(new TopicPartition("empty-topic", 0), 0L);
        var endOffsets = Map.of(new TopicPartition("empty-topic", 0), 0L);

        var topic = new Topic(topicDescription, beginOffsets, endOffsets);

        assertEquals(0L, topic.getRecordCount());
    }

    @Test
    void shouldCalculateReplicaCount() {
        var node1 = new Node(1, "localhost", 9092);
        var node2 = new Node(2, "localhost", 9093);
        var node3 = new Node(3, "localhost", 9094);
        var topicDescription = new TopicDescription("test-topic", false, List.of(
                new TopicPartitionInfo(0, node1, List.of(node1, node2, node3), List.of(node1, node2, node3))
        ), Collections.emptySet(), Uuid.randomUuid());

        var beginOffsets = Map.of(new TopicPartition("test-topic", 0), 0L);
        var endOffsets = Map.of(new TopicPartition("test-topic", 0), 100L);

        var topic = new Topic(topicDescription, beginOffsets, endOffsets);

        assertEquals(3, topic.getReplicaCount());
    }

    @Test
    void shouldCalculateMaxReplicaCountAcrossPartitions() {
        var node1 = new Node(1, "localhost", 9092);
        var node2 = new Node(2, "localhost", 9093);
        var node3 = new Node(3, "localhost", 9094);
        var topicDescription = new TopicDescription("test-topic", false, List.of(
                new TopicPartitionInfo(0, node1, List.of(node1, node2), List.of(node1, node2)),
                new TopicPartitionInfo(1, node1, List.of(node1, node2, node3), List.of(node1, node2, node3))
        ), Collections.emptySet(), Uuid.randomUuid());

        var beginOffsets = Map.of(
                new TopicPartition("test-topic", 0), 0L,
                new TopicPartition("test-topic", 1), 0L
        );
        var endOffsets = Map.of(
                new TopicPartition("test-topic", 0), 100L,
                new TopicPartition("test-topic", 1), 100L
        );

        var topic = new Topic(topicDescription, beginOffsets, endOffsets);

        assertEquals(3, topic.getReplicaCount());
    }

    @Test
    void shouldCalculateReplicaCountAsZeroForEmptyPartitions() {
        var topicDescription = new TopicDescription("test-topic", false, Collections.emptyList(), Collections.emptySet(), Uuid.randomUuid());

        var topic = new Topic(topicDescription, Collections.emptyMap(), Collections.emptyMap());

        assertEquals(0, topic.getReplicaCount());
    }

    @Test
    void shouldCalculateInSyncReplicaCount() {
        var node1 = new Node(1, "localhost", 9092);
        var node2 = new Node(2, "localhost", 9093);
        var node3 = new Node(3, "localhost", 9094);
        var topicDescription = new TopicDescription("test-topic", false, List.of(
                new TopicPartitionInfo(0, node1, List.of(node1, node2, node3), List.of(node1, node2))
        ), Collections.emptySet(), Uuid.randomUuid());

        var beginOffsets = Map.of(new TopicPartition("test-topic", 0), 0L);
        var endOffsets = Map.of(new TopicPartition("test-topic", 0), 100L);

        var topic = new Topic(topicDescription, beginOffsets, endOffsets);

        assertEquals(2, topic.getInSyncReplicaCount());
    }

    @Test
    void shouldCalculateMinInSyncReplicaCountAcrossPartitions() {
        var node1 = new Node(1, "localhost", 9092);
        var node2 = new Node(2, "localhost", 9093);
        var node3 = new Node(3, "localhost", 9094);
        var topicDescription = new TopicDescription("test-topic", false, List.of(
                new TopicPartitionInfo(0, node1, List.of(node1, node2, node3), List.of(node1, node2, node3)),
                new TopicPartitionInfo(1, node1, List.of(node1, node2, node3), List.of(node1))
        ), Collections.emptySet(), Uuid.randomUuid());

        var beginOffsets = Map.of(
                new TopicPartition("test-topic", 0), 0L,
                new TopicPartition("test-topic", 1), 0L
        );
        var endOffsets = Map.of(
                new TopicPartition("test-topic", 0), 100L,
                new TopicPartition("test-topic", 1), 100L
        );

        var topic = new Topic(topicDescription, beginOffsets, endOffsets);

        assertEquals(1, topic.getInSyncReplicaCount());
    }

    @Test
    void shouldReturnTopicMetadata() {
        var node = new Node(1, "localhost", 9092);
        var topicId = Uuid.randomUuid();
        var topicDescription = new TopicDescription("my-topic", true, List.of(
                new TopicPartitionInfo(0, node, List.of(node), List.of(node))
        ), Collections.emptySet(), topicId);

        var beginOffsets = Map.of(new TopicPartition("my-topic", 0), 0L);
        var endOffsets = Map.of(new TopicPartition("my-topic", 0), 100L);

        var topic = new Topic(topicDescription, beginOffsets, endOffsets);

        assertEquals("my-topic", topic.getName());
        assertEquals(topicId.toString(), topic.getId());
        assertTrue(topic.isInternal());
        assertEquals(1, topic.getPartitions().size());
    }

    @Test
    void shouldCreatePartitionWithOffsets() {
        var node = new Node(1, "localhost", 9092);
        var topicDescription = new TopicDescription("test-topic", false, List.of(
                new TopicPartitionInfo(0, node, List.of(node), List.of(node))
        ), Collections.emptySet(), Uuid.randomUuid());

        var tp = new TopicPartition("test-topic", 0);
        var beginOffsets = Map.of(tp, 50L);
        var endOffsets = Map.of(tp, 150L);

        var topic = new Topic(topicDescription, beginOffsets, endOffsets);
        var partition = topic.getPartitions().getFirst();

        assertEquals(0, partition.getId());
        assertEquals(50L, partition.getBeginOffset());
        assertEquals(150L, partition.getEndOffset());
        assertEquals(tp, partition.getTp());
    }

    @Test
    void shouldCreatePartitionReplicas() {
        var leader = new Node(1, "localhost", 9092);
        var follower1 = new Node(2, "localhost", 9093);
        var follower2 = new Node(3, "localhost", 9094);
        var topicDescription = new TopicDescription("test-topic", false, List.of(
                new TopicPartitionInfo(0, leader, List.of(leader, follower1, follower2), List.of(leader, follower1))
        ), Collections.emptySet(), Uuid.randomUuid());

        var tp = new TopicPartition("test-topic", 0);
        var topic = new Topic(topicDescription, Map.of(tp, 0L), Map.of(tp, 100L));
        var replicas = topic.getPartitions().getFirst().getReplicas();

        assertEquals(3, replicas.size());

        var leaderReplica = replicas.stream().filter(Topic.Partition.Replica::isLeader).findFirst();
        assertTrue(leaderReplica.isPresent());
        assertEquals(1, leaderReplica.get().getId());
        assertTrue(leaderReplica.get().isInSync());

        var outOfSyncReplica = replicas.stream().filter(r -> r.getId() == 3).findFirst();
        assertTrue(outOfSyncReplica.isPresent());
        assertFalse(outOfSyncReplica.get().isLeader());
        assertFalse(outOfSyncReplica.get().isInSync());
    }

    @Test
    void shouldHandleNullLeader() {
        var node = new Node(1, "localhost", 9092);
        var topicDescription = new TopicDescription("test-topic", false, List.of(
                new TopicPartitionInfo(0, null, List.of(node), List.of(node))
        ), Collections.emptySet(), Uuid.randomUuid());

        var tp = new TopicPartition("test-topic", 0);
        var topic = new Topic(topicDescription, Map.of(tp, 0L), Map.of(tp, 100L));
        var replicas = topic.getPartitions().getFirst().getReplicas();

        assertEquals(1, replicas.size());
        assertFalse(replicas.getFirst().isLeader());
    }
}
