package com.prafka.core.service;

import com.prafka.core.model.Quota;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterClientQuotasResult;
import org.apache.kafka.clients.admin.DescribeClientQuotasResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.quota.ClientQuotaAlteration;
import org.apache.kafka.common.quota.ClientQuotaEntity;
import org.apache.kafka.common.quota.ClientQuotaFilter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class QuotaServiceTest {

    private AdminClient adminClient = mock(AdminClient.class);
    private QuotaService quotaService = new QuotaService() {
        @Override
        protected AdminClient adminClient(String clusterId) {
            return adminClient;
        }
    };

    @Test
    void shouldGetAll() throws Exception {
        // Given
        var clusterId = "test-cluster";

        var quotaEntities = new HashMap<ClientQuotaEntity, Map<String, Double>>();

        var userEntity = new ClientQuotaEntity(Map.of(ClientQuotaEntity.USER, "test-user"));
        var userConfigs = Map.of("producer_byte_rate", 1024.0, "consumer_byte_rate", 2048.0);
        quotaEntities.put(userEntity, userConfigs);

        var clientIdEntity = new ClientQuotaEntity(Map.of(ClientQuotaEntity.CLIENT_ID, "test-client"));
        var clientIdConfigs = Map.of("request_percentage", 50.0);
        quotaEntities.put(clientIdEntity, clientIdConfigs);

        var describeClientQuotasResult = mock(DescribeClientQuotasResult.class);
        when(describeClientQuotasResult.entities()).thenReturn(KafkaFuture.completedFuture(quotaEntities));
        when(adminClient.describeClientQuotas(any(ClientQuotaFilter.class))).thenReturn(describeClientQuotasResult);

        // When
        var result = quotaService.getAll(clusterId).get();

        // Then
        assertEquals(3, result.size());

        var userProducerQuota = result.stream()
                .filter(q -> q.getEntity().getName().equals("test-user") &&
                        q.getConfig().getInternalType() == Quota.ConfigType.PRODUCER_RATE)
                .findFirst().orElse(null);
        assertNotNull(userProducerQuota);
        assertEquals("test-user", userProducerQuota.getEntity().getName());
        assertEquals(Quota.EntityType.USER, userProducerQuota.getEntity().getInternalType());
        assertEquals(BigDecimal.valueOf(1024.0), userProducerQuota.getConfig().getValue());
        assertEquals(Quota.ConfigType.PRODUCER_RATE, userProducerQuota.getConfig().getInternalType());

        var userConsumerQuota = result.stream()
                .filter(q -> q.getEntity().getName().equals("test-user") &&
                        q.getConfig().getInternalType() == Quota.ConfigType.CONSUMER_RATE)
                .findFirst().orElse(null);
        assertNotNull(userConsumerQuota);
        assertEquals("test-user", userConsumerQuota.getEntity().getName());
        assertEquals(Quota.EntityType.USER, userConsumerQuota.getEntity().getInternalType());
        assertEquals(BigDecimal.valueOf(2048.0), userConsumerQuota.getConfig().getValue());
        assertEquals(Quota.ConfigType.CONSUMER_RATE, userConsumerQuota.getConfig().getInternalType());

        var clientIdQuota = result.stream()
                .filter(q -> q.getEntity().getName().equals("test-client") &&
                        q.getConfig().getInternalType() == Quota.ConfigType.REQUEST_PERCENTAGE)
                .findFirst().orElse(null);
        assertNotNull(clientIdQuota);
        assertEquals("test-client", clientIdQuota.getEntity().getName());
        assertEquals(Quota.EntityType.CLIENT_ID, clientIdQuota.getEntity().getInternalType());
        assertEquals(BigDecimal.valueOf(50.0), clientIdQuota.getConfig().getValue());
        assertEquals(Quota.ConfigType.REQUEST_PERCENTAGE, clientIdQuota.getConfig().getInternalType());
    }

    @Test
    void shouldCreateQuota() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var entityType = Quota.EntityType.USER;
        var entityName = "test-user";
        var configType = Quota.ConfigType.PRODUCER_RATE;
        var configValue = 1024.0;

        var alterClientQuotasResult = mock(AlterClientQuotasResult.class);
        when(alterClientQuotasResult.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(adminClient.alterClientQuotas(anyList())).thenReturn(alterClientQuotasResult);

        // When
        quotaService.create(clusterId, entityType, entityName, configType, configValue).get();

        // Then
        var expectedEntity = new ClientQuotaEntity(Map.of(ClientQuotaEntity.USER, "test-user"));
        var expectedOp = new ClientQuotaAlteration.Op("producer_byte_rate", 1024.0);

        verify(adminClient).alterClientQuotas(argThat(result -> {
            var alterations = new ArrayList<>(result);
            if (alterations.size() != 1) return false;
            var alteration = alterations.get(0);
            var ops = new ArrayList<>(alteration.ops());
            return alteration.entity().entries().equals(expectedEntity.entries()) &&
                    ops.size() == 1 &&
                    ops.get(0).key().equals(expectedOp.key()) &&
                    ops.get(0).value().equals(expectedOp.value());
        }));
    }

    @Test
    void shouldGetAllQuotasSummary() throws Exception {
        // Given
        var clusterId = "test-cluster";

        var quotaEntities = new HashMap<ClientQuotaEntity, Map<String, Double>>();

        var userEntity = new ClientQuotaEntity(Map.of(ClientQuotaEntity.USER, "test-user"));
        var userConfigs = Map.of(
                "producer_byte_rate", 1024.0,
                "consumer_byte_rate", 2048.0
        );
        quotaEntities.put(userEntity, userConfigs);

        var clientIdEntity = new ClientQuotaEntity(Map.of(ClientQuotaEntity.CLIENT_ID, "test-client"));
        var clientIdConfigs = Map.of("request_percentage", 50.0);
        quotaEntities.put(clientIdEntity, clientIdConfigs);

        var clientAndUserEntity = new ClientQuotaEntity(Map.of(
                ClientQuotaEntity.CLIENT_ID, "test-client",
                ClientQuotaEntity.USER, "test-user"
        ));
        var clientAndUserConfigs = Map.of(
                "connection_creation_rate", 10.0,
                "controller_mutation_rate", 5.0
        );
        quotaEntities.put(clientAndUserEntity, clientAndUserConfigs);

        var describeClientQuotasResult = mock(DescribeClientQuotasResult.class);
        when(describeClientQuotasResult.entities()).thenReturn(KafkaFuture.completedFuture(quotaEntities));
        when(adminClient.describeClientQuotas(any(ClientQuotaFilter.class))).thenReturn(describeClientQuotasResult);

        // When
        var result = quotaService.getAllQuotasSummary(clusterId).get();

        // Then
        assertEquals(7, result.quotaCount());
        assertEquals(1, result.producerRateCount());
        assertEquals(1, result.consumerRateCount());
        assertEquals(2, result.connectionRateCount());
        assertEquals(2, result.controllerRateCount());
        assertEquals(1, result.requestPercentCount());
    }
}