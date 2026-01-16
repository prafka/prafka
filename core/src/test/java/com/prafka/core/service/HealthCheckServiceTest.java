package com.prafka.core.service;

import com.prafka.core.manager.KafkaManager;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.Test;
import org.sourcelab.kafka.connect.apiclient.KafkaConnectClient;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectServerVersion;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthCheckServiceTest {

    private AdminClient adminClient = mock(AdminClient.class);
    private SchemaRegistryClient schemaRegistryClient = mock(SchemaRegistryClient.class);
    private KafkaConnectClient kafkaConnectClient = mock(KafkaConnectClient.class);
    private KafkaManager kafkaManager = mock(KafkaManager.class);
    private HealthCheckService healthCheckService = new HealthCheckService() {
        @Override
        protected AdminClient adminClient(String clusterId) {
            return adminClient;
        }
        @Override
        protected SchemaRegistryClient schemaRegistryClient(String clusterId) {
            return schemaRegistryClient;
        }
        @Override
        protected Map<String, KafkaConnectClient> connectClients(String clusterId) {
            return Map.of("connect1", kafkaConnectClient);
        }
        @Override
        protected KafkaConnectClient connectClient(String clusterId, String connectId) {
            return kafkaConnectClient;
        }
    };

    @Test
    void shouldCheckAllAvailable() throws Exception {
        // Given
        var clusterId = "test-cluster";

        var mockDescribeClusterResult = mock(DescribeClusterResult.class);
        when(mockDescribeClusterResult.clusterId()).thenReturn(KafkaFuture.completedFuture(clusterId));
        when(adminClient.describeCluster()).thenReturn(mockDescribeClusterResult);
        when(schemaRegistryClient.getMode()).thenReturn("READWRITE");
        when(kafkaConnectClient.getConnectServerVersion()).thenReturn(new ConnectServerVersion());
        when(kafkaManager.schemaRegistryIsDefined(clusterId)).thenReturn(true);
        when(kafkaManager.connectsIsDefined(clusterId)).thenReturn(true);

        healthCheckService.setKafkaManager(kafkaManager);

        // When
        var result = healthCheckService.isAvailable(clusterId).get();

        // Then
        assertTrue(result.cluster().available());
        assertTrue(result.schemaRegistry().isPresent());
        assertTrue(result.schemaRegistry().get().available());
        assertTrue(result.connects().isPresent());
        assertTrue(result.connects().get().get("connect1").available());
    }
}