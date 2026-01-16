package com.prafka.core.service;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.protocol.Errors;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LogDirServiceTest {

    private AdminClient adminClient = mock(AdminClient.class);
    private LogDirService logDirService = new LogDirService() {
        @Override
        protected AdminClient adminClient(String clusterId) {
            return adminClient;
        }
    };

    @Test
    void shouldGetAllByBrokers() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var brokerIds = Arrays.asList(1, 2);

        var mockLogDirs = new HashMap<Integer, Map<String, LogDirDescription>>();

        var broker1Dirs = new HashMap<String, LogDirDescription>();
        var broker1Replicas = new HashMap<TopicPartition, ReplicaInfo>();
        broker1Replicas.put(new TopicPartition("topic1", 0), new ReplicaInfo(1000L, 500L, true));
        broker1Replicas.put(new TopicPartition("topic1", 1), new ReplicaInfo(2000L, 1000L, false));
        broker1Dirs.put("/tmp/kafka-logs", new LogDirDescription(Errors.forCode((short) 0).exception(), broker1Replicas));
        mockLogDirs.put(1, broker1Dirs);

        var broker2Dirs = new HashMap<String, LogDirDescription>();
        var broker2Replicas = new HashMap<TopicPartition, ReplicaInfo>();
        broker2Replicas.put(new TopicPartition("topic1", 0), new ReplicaInfo(1000L, 500L, false));
        broker2Replicas.put(new TopicPartition("topic1", 1), new ReplicaInfo(2000L, 1000L, true));
        broker2Dirs.put("/tmp/kafka-logs-2", new LogDirDescription(Errors.forCode((short) 0).exception(), broker2Replicas));
        mockLogDirs.put(2, broker2Dirs);

        var describeLogDirsResult = mock(DescribeLogDirsResult.class);
        when(describeLogDirsResult.allDescriptions()).thenReturn(KafkaFuture.completedFuture(mockLogDirs));
        when(adminClient.describeLogDirs(any())).thenReturn(describeLogDirsResult);

        // When
        var result = logDirService.getAllByBrokers(clusterId, brokerIds).get();

        // Then
        assertEquals(2, result.size());
        assertTrue(result.containsKey(1));
        assertTrue(result.containsKey(2));

        var broker1Result = result.get(1);
        assertEquals(2, broker1Result.size());
        assertTrue(broker1Result.containsKey(new TopicPartition("topic1", 0)));
        assertTrue(broker1Result.containsKey(new TopicPartition("topic1", 1)));

        var broker1Partition0LogDirs = broker1Result.get(new TopicPartition("topic1", 0));
        assertEquals(1, broker1Partition0LogDirs.size());
        assertEquals(1, broker1Partition0LogDirs.get(0).getBrokerId());
        assertEquals("/tmp/kafka-logs", broker1Partition0LogDirs.get(0).getPath());
        assertEquals(1000L, broker1Partition0LogDirs.get(0).getSize());
        assertEquals(500L, broker1Partition0LogDirs.get(0).getOffsetLag());

        var broker2Result = result.get(2);
        assertEquals(2, broker2Result.size());
        assertTrue(broker2Result.containsKey(new TopicPartition("topic1", 0)));
        assertTrue(broker2Result.containsKey(new TopicPartition("topic1", 1)));

        var broker2Partition1LogDirs = broker2Result.get(new TopicPartition("topic1", 1));
        assertEquals(1, broker2Partition1LogDirs.size());
        assertEquals(2, broker2Partition1LogDirs.get(0).getBrokerId());
        assertEquals("/tmp/kafka-logs-2", broker2Partition1LogDirs.get(0).getPath());
        assertEquals(2000L, broker2Partition1LogDirs.get(0).getSize());
        assertEquals(1000L, broker2Partition1LogDirs.get(0).getOffsetLag());
    }

    @Test
    void shouldGetAllByTopics() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var topicNames = Arrays.asList("topic1", "topic2");

        var mockLogDirs = new HashMap<Integer, Map<String, LogDirDescription>>();

        var broker1Dirs = new HashMap<String, LogDirDescription>();
        var broker1Replicas = new HashMap<TopicPartition, ReplicaInfo>();
        broker1Replicas.put(new TopicPartition("topic1", 0), new ReplicaInfo(1000L, 500L, true));
        broker1Replicas.put(new TopicPartition("topic2", 0), new ReplicaInfo(2000L, 1000L, false));
        broker1Dirs.put("/tmp/kafka-logs", new LogDirDescription(Errors.forCode((short) 0).exception(), broker1Replicas));
        mockLogDirs.put(1, broker1Dirs);

        var broker2Dirs = new HashMap<String, LogDirDescription>();
        var broker2Replicas = new HashMap<TopicPartition, ReplicaInfo>();
        broker2Replicas.put(new TopicPartition("topic1", 0), new ReplicaInfo(1000L, 500L, false));
        broker2Replicas.put(new TopicPartition("topic2", 0), new ReplicaInfo(2000L, 1000L, true));
        broker2Dirs.put("/tmp/kafka-logs-2", new LogDirDescription(Errors.forCode((short) 0).exception(), broker2Replicas));
        mockLogDirs.put(2, broker2Dirs);

        var describeLogDirsResult = mock(DescribeLogDirsResult.class);
        when(describeLogDirsResult.allDescriptions()).thenReturn(KafkaFuture.completedFuture(mockLogDirs));
        when(adminClient.describeLogDirs(any())).thenReturn(describeLogDirsResult);

        var nodeList = Arrays.asList(new Node(1, "host1", 9092), new Node(2, "host2", 9092));
        var describeClusterResult = mock(DescribeClusterResult.class);
        when(describeClusterResult.nodes()).thenReturn(KafkaFuture.completedFuture(nodeList));
        when(adminClient.describeCluster()).thenReturn(describeClusterResult);

        // When
        var result = logDirService.getAllByTopics(clusterId, topicNames).get();

        // Then
        assertEquals(2, result.size());
        assertTrue(result.containsKey("topic1"));
        assertTrue(result.containsKey("topic2"));

        var topic1Result = result.get("topic1");
        assertEquals(1, topic1Result.size());
        assertTrue(topic1Result.containsKey(new TopicPartition("topic1", 0)));

        var topic1Partition0LogDirs = topic1Result.get(new TopicPartition("topic1", 0));
        assertEquals(2, topic1Partition0LogDirs.size());

        var broker1LogDir = topic1Partition0LogDirs.stream().filter(logDir -> logDir.getBrokerId() == 1).findFirst().get();
        assertEquals(1, broker1LogDir.getBrokerId());
        assertEquals("/tmp/kafka-logs", broker1LogDir.getPath());
        assertEquals(1000L, broker1LogDir.getSize());
        assertEquals(500L, broker1LogDir.getOffsetLag());

        var broker2LogDir = topic1Partition0LogDirs.stream().filter(logDir -> logDir.getBrokerId() == 2).findFirst().get();
        assertEquals(2, broker2LogDir.getBrokerId());
        assertEquals("/tmp/kafka-logs-2", broker2LogDir.getPath());
        assertEquals(1000L, broker2LogDir.getSize());
        assertEquals(500L, broker2LogDir.getOffsetLag());

        var topic2Result = result.get("topic2");
        assertEquals(1, topic2Result.size());
        assertTrue(topic2Result.containsKey(new TopicPartition("topic2", 0)));

        var topic2Partition0LogDirs = topic2Result.get(new TopicPartition("topic2", 0));
        assertEquals(2, topic2Partition0LogDirs.size());

        var topic2Broker1LogDir = topic2Partition0LogDirs.stream().filter(logDir -> logDir.getBrokerId() == 1).findFirst().get();
        assertEquals(1, topic2Broker1LogDir.getBrokerId());
        assertEquals("/tmp/kafka-logs", topic2Broker1LogDir.getPath());
        assertEquals(2000L, topic2Broker1LogDir.getSize());
        assertEquals(1000L, topic2Broker1LogDir.getOffsetLag());

        var topic2Broker2LogDir = topic2Partition0LogDirs.stream().filter(logDir -> logDir.getBrokerId() == 2).findFirst().get();
        assertEquals(2, topic2Broker2LogDir.getBrokerId());
        assertEquals("/tmp/kafka-logs-2", topic2Broker2LogDir.getPath());
        assertEquals(2000L, topic2Broker2LogDir.getSize());
        assertEquals(1000L, topic2Broker2LogDir.getOffsetLag());
    }

    @Test
    void shouldGetAllTopicsSummary() throws Exception {
        // Given
        var clusterId = "test-cluster";

        var nodeList = Arrays.asList(new Node(1, "host1", 9092), new Node(2, "host2", 9092));
        var describeClusterResult = mock(DescribeClusterResult.class);
        when(describeClusterResult.nodes()).thenReturn(KafkaFuture.completedFuture(nodeList));
        when(adminClient.describeCluster()).thenReturn(describeClusterResult);

        var mockLogDirs = new HashMap<Integer, Map<String, LogDirDescription>>();

        var broker1Dirs = new HashMap<String, LogDirDescription>();
        var broker1Replicas = new HashMap<TopicPartition, ReplicaInfo>();
        broker1Replicas.put(new TopicPartition("topic1", 0), new ReplicaInfo(1000L, 500L, true));
        broker1Replicas.put(new TopicPartition("topic2", 0), new ReplicaInfo(2000L, 1000L, false));
        broker1Dirs.put("/tmp/kafka-logs", new LogDirDescription(Errors.forCode((short) 0).exception(), broker1Replicas));
        mockLogDirs.put(1, broker1Dirs);

        var broker2Dirs = new HashMap<String, LogDirDescription>();
        var broker2Replicas = new HashMap<TopicPartition, ReplicaInfo>();
        broker2Replicas.put(new TopicPartition("topic1", 0), new ReplicaInfo(1000L, 500L, false));
        broker2Replicas.put(new TopicPartition("topic2", 0), new ReplicaInfo(2000L, 1000L, true));
        broker2Dirs.put("/tmp/kafka-logs-2", new LogDirDescription(Errors.forCode((short) 0).exception(), broker2Replicas));
        mockLogDirs.put(2, broker2Dirs);

        var describeLogDirsResult = mock(DescribeLogDirsResult.class);
        when(describeLogDirsResult.allDescriptions()).thenReturn(KafkaFuture.completedFuture(mockLogDirs));
        when(adminClient.describeLogDirs(any())).thenReturn(describeLogDirsResult);

        var listTopicsResult = mock(ListTopicsResult.class);
        when(listTopicsResult.names()).thenReturn(KafkaFuture.completedFuture(Set.of("topic1", "topic2")));
        when(adminClient.listTopics(any(ListTopicsOptions.class))).thenReturn(listTopicsResult);

        // When
        var result = logDirService.getAllTopicsSummary(clusterId).get();

        // Then
        assertEquals(6000L, result.size());
    }
}