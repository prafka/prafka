package com.prafka.core.service;

import com.prafka.core.model.Connector;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sourcelab.kafka.connect.apiclient.KafkaConnectClient;
import org.sourcelab.kafka.connect.apiclient.request.dto.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConnectServiceTest {

    private KafkaConnectClient kafkaConnectClient = mock(KafkaConnectClient.class);
    private ConnectService connectService = new ConnectService() {
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
    void shouldGetAllNames() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var connectId = "test-connect";

        when(kafkaConnectClient.getConnectors()).thenReturn(List.of("connector1", "connector2", "connector3"));

        // When
        var result = connectService.getAllNames(clusterId, connectId).get();

        // Then
        assertEquals(3, result.size());
        assertEquals(new Connector.Name(connectId, "connector1"), result.get(0));
        assertEquals(new Connector.Name(connectId, "connector2"), result.get(1));
        assertEquals(new Connector.Name(connectId, "connector3"), result.get(2));
    }

    @Test
    void shouldGet() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var connectId = "test-connect";
        var connectorName = "test-connector";
        var cn = new Connector.Name(connectId, connectorName);

        var mockDefinition = mock(ConnectorDefinition.class);
        var mockStatus = mock(ConnectorStatus.class);
        var mockTopics = mock(ConnectorTopics.class);

        when(mockDefinition.getName()).thenReturn(connectorName);
        when(mockDefinition.getType()).thenReturn("SINK");
        when(mockStatus.getConnector()).thenReturn(Map.of("state", "RUNNING"));

        when(kafkaConnectClient.getConnector(connectorName)).thenReturn(mockDefinition);
        when(kafkaConnectClient.getConnectorStatus(connectorName)).thenReturn(mockStatus);
        when(kafkaConnectClient.getConnectorTopics(connectorName)).thenReturn(mockTopics);

        // When
        var result = connectService.get(clusterId, cn).get();

        // Then
        assertEquals(mockDefinition.getName(), result.getName());
    }

    @Test
    void shouldCreate() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var connectId = "test-connect";
        var name = "test-connector";
        var config = Map.of("key1", "value1", "key2", "value2");

        // When
        connectService.create(clusterId, connectId, name, config).get();

        // Then
        var captor = ArgumentCaptor.forClass(NewConnectorDefinition.class);
        verify(kafkaConnectClient).addConnector(captor.capture());
        var captured = captor.getValue();
        assertEquals(name, captured.getName());
        assertEquals(config, captured.getConfig());
    }

    @Test
    void shouldValidateSuccess() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var connectId = "test-connect";
        var plugin = "test-plugin";
        var config = Map.of("key1", "value1", "key2", "value2");

        var mockResult = mock(ConnectorPluginConfigValidationResults.class);
        when(mockResult.getErrorCount()).thenReturn(0);
        when(kafkaConnectClient.validateConnectorPluginConfig(any())).thenReturn(mockResult);

        // When
        connectService.validate(clusterId, connectId, plugin, config).get();

        // Then
        var captor = ArgumentCaptor.forClass(ConnectorPluginConfigDefinition.class);
        verify(kafkaConnectClient).validateConnectorPluginConfig(captor.capture());
        var captured = captor.getValue();
        assertEquals(plugin, captured.getName());
        assertEquals(config, captured.getConfig());
    }

    @Test
    void shouldValidateError() {
        // Given
        var clusterId = "test-cluster";
        var connectId = "test-connect";
        var plugin = "test-plugin";
        var config = Map.of("key1", "value1", "key2", "value2");

        var mockResult = mock(ConnectorPluginConfigValidationResults.class);
        when(mockResult.getErrorCount()).thenReturn(1);
        when(kafkaConnectClient.validateConnectorPluginConfig(any())).thenReturn(mockResult);

        // When
        var result = connectService.validate(clusterId, connectId, plugin, config);

        // Then
        var exception = assertThrows(ExecutionException.class, result::get);
        assertInstanceOf(ConnectService.ConnectorValidateError.class, exception.getCause());

        var captor = ArgumentCaptor.forClass(ConnectorPluginConfigDefinition.class);
        verify(kafkaConnectClient).validateConnectorPluginConfig(captor.capture());
        var captured = captor.getValue();
        assertEquals(plugin, captured.getName());
        assertEquals(config, captured.getConfig());
    }

    @Test
    void shouldGetAllConnectorsSummary() throws Exception {
        // Given
        var clusterId = "test-cluster";

        var mockExpandedStatus = mock(ConnectorsWithExpandedStatus.class);
        var mockStatus1 = mock(ConnectorStatus.class);
        var mockStatus2 = mock(ConnectorStatus.class);

        var task1 = new ConnectorStatus.TaskStatus(1, "RUNNING", "worker1", null);
        var task2 = new ConnectorStatus.TaskStatus(2, "FAILED", "worker1", null);
        var task3 = new ConnectorStatus.TaskStatus(3, "RUNNING", "worker2", null);

        when(mockStatus1.getConnector()).thenReturn(Map.of("state", "RUNNING"));
        when(mockStatus1.getTasks()).thenReturn(List.of(task1, task2));

        when(mockStatus2.getConnector()).thenReturn(Map.of("state", "FAILED"));
        when(mockStatus2.getTasks()).thenReturn(List.of(task3));

        when(mockExpandedStatus.getAllStatuses()).thenReturn(List.of(mockStatus1, mockStatus2));
        when(kafkaConnectClient.getConnectorsWithExpandedStatus()).thenReturn(mockExpandedStatus);

        // When
        var result = connectService.getAllConnectorsSummary(clusterId).get();

        // Then
        assertEquals(2, result.connectorCount());
        assertEquals(1, result.runConnectorCount());
        assertEquals(1, result.failConnectorCount());
        assertEquals(2, result.runTaskCount());
        assertEquals(1, result.failTaskCount());
    }
}