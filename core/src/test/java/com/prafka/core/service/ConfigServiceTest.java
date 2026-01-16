package com.prafka.core.service;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class ConfigServiceTest {

    private AdminClient adminClient = mock(AdminClient.class);
    private ConfigService configService = new ConfigService() {
        @Override
        protected AdminClient adminClient(String clusterId) {
            return adminClient;
        }
    };

    @Test
    void shouldGetAllByBroker() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var brokerId = 1;
        var configResource = new ConfigResource(ConfigResource.Type.BROKER, String.valueOf(brokerId));

        var entry1 = new ConfigEntry("config1", "value1");
        var entry2 = new ConfigEntry("config2", "value2");
        var config = new Config(List.of(entry1, entry2));

        var describeConfigsResult = mock(DescribeConfigsResult.class);
        when(describeConfigsResult.all()).thenReturn(KafkaFuture.completedFuture(Map.of(configResource, config)));
        when(adminClient.describeConfigs(anyList(), any())).thenReturn(describeConfigsResult);

        // When
        var result = configService.getAllByBroker(clusterId, brokerId).get();

        // Then
        assertEquals(2, result.size());
        ArgumentCaptor<List<ConfigResource>> resourceCaptor = ArgumentCaptor.forClass(List.class);
        verify(adminClient).describeConfigs(resourceCaptor.capture(), any());
        assertEquals(configResource, resourceCaptor.getValue().getFirst());
    }

    @Test
    void shouldSetByTopic() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var topicName = "test-topic";
        var configName = "cleanup.policy";
        var configValue = "compact";

        var alterConfigsResult = mock(AlterConfigsResult.class);
        when(alterConfigsResult.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(adminClient.incrementalAlterConfigs(anyMap())).thenReturn(alterConfigsResult);

        // When
        configService.setByTopic(clusterId, topicName, configName, configValue).get();

        // Then
        ArgumentCaptor<Map<ConfigResource, Collection<AlterConfigOp>>> configsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(adminClient).incrementalAlterConfigs(configsCaptor.capture());

        var capturedConfigList = configsCaptor.getValue();
        assertEquals(1, capturedConfigList.size());

        var configResource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
        assertTrue(capturedConfigList.containsKey(configResource));

        var opList = capturedConfigList.get(configResource);
        assertEquals(1, opList.size());

        var op = opList.iterator().next();
        assertEquals(configName, op.configEntry().name());
        assertEquals(configValue, op.configEntry().value());
        assertEquals(AlterConfigOp.OpType.SET, op.opType());
    }
}