package com.prafka.core.service;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrokerServiceTest {

    private AdminClient adminClient = mock(AdminClient.class);
    private BrokerService brokerService = new BrokerService() {
        @Override
        protected AdminClient adminClient(String clusterId) {
            return adminClient;
        }
    };

    @Test
    void shouldGetAll() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var controllerNode = new Node(1, "controller-host", 9092);
        var brokerNode1 = new Node(1, "host1", 9092);
        var brokerNode2 = new Node(2, "host2", 9092);
        var nodes = List.of(brokerNode1, brokerNode2);
        var describeClusterResult = mock(DescribeClusterResult.class);

        when(adminClient.describeCluster()).thenReturn(describeClusterResult);
        when(describeClusterResult.controller()).thenReturn(KafkaFuture.completedFuture(controllerNode));
        when(describeClusterResult.nodes()).thenReturn(KafkaFuture.completedFuture(nodes));

        // When
        var result = brokerService.getAll(clusterId).get();

        // Then
        assertEquals(2, result.size());
        assertTrue(result.get(0).isController());
        assertFalse(result.get(1).isController());
    }

    @Test
    void shouldGetAllBrokersSummary() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var controllerNode = new Node(1, "controller-host", 9092);
        var brokerNode1 = new Node(1, "host1", 9092);
        var brokerNode2 = new Node(2, "host2", 9092);
        var nodes = List.of(brokerNode1, brokerNode2);
        var describeClusterResult = mock(DescribeClusterResult.class);

        when(adminClient.describeCluster()).thenReturn(describeClusterResult);
        when(describeClusterResult.controller()).thenReturn(KafkaFuture.completedFuture(controllerNode));
        when(describeClusterResult.nodes()).thenReturn(KafkaFuture.completedFuture(nodes));

        // When
        var result = brokerService.getAllBrokersSummary(clusterId).get();

        // Then
        assertEquals(2, result.brokerCount());
        assertTrue(result.controller());
    }
}