package com.prafka.core.service;

import com.prafka.core.model.Quota;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.kafka.common.quota.ClientQuotaAlteration;
import org.apache.kafka.common.quota.ClientQuotaEntity;
import org.apache.kafka.common.quota.ClientQuotaFilter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Named
@Singleton
public class QuotaService extends AbstractService {

    public CompletableFuture<List<Quota>> getAll(String clusterId) {
        return adminClient(clusterId)
                .describeClientQuotas(ClientQuotaFilter.all())
                .entities()
                .thenApply(map ->
                        map.entrySet().stream()
                                .flatMap(entities ->
                                        entities.getKey().entries().entrySet().stream()
                                                .flatMap(entity ->
                                                        entities.getValue().entrySet().stream()
                                                                .map(config -> new Quota(entity, config))
                                                )
                                )
                                .toList()
                )
                .toCompletionStage()
                .toCompletableFuture();
    }

    public CompletableFuture<Void> create(String clusterId, Quota.EntityType entityType, String entityName, Quota.ConfigType configType, Double configValue) {
        var entity = new ClientQuotaEntity(new HashMap<>() {{
            put(entityType.getValue(), entityName);
        }});
        var config = new ClientQuotaAlteration.Op(configType.getValue(), configValue);
        return alterQuota(clusterId, entity, config);
    }

    public CompletableFuture<Void> update(String clusterId, Quota.EntityType entityType, String entityName, Quota.ConfigType configType, Double configValue) {
        var entity = new ClientQuotaEntity(new HashMap<>() {{
            put(entityType.getValue(), entityName);
        }});
        var config = new ClientQuotaAlteration.Op(configType.getValue(), configValue);
        return alterQuota(clusterId, entity, config);
    }

    public CompletableFuture<Void> delete(String clusterId, Quota.EntityType entityType, String entityName, Quota.ConfigType configType) {
        var entity = new ClientQuotaEntity(new HashMap<>() {{
            put(entityType.getValue(), entityName);
        }});
        var config = new ClientQuotaAlteration.Op(configType.getValue(), null);
        return alterQuota(clusterId, entity, config);
    }

    private CompletableFuture<Void> alterQuota(String clusterId, ClientQuotaEntity entity, ClientQuotaAlteration.Op config) {
        return adminClient(clusterId)
                .alterClientQuotas(Collections.singletonList(new ClientQuotaAlteration(entity, Collections.singletonList(config))))
                .all()
                .toCompletionStage()
                .toCompletableFuture();
    }

    public record AllQuotasSummary(int quotaCount, long producerRateCount, long consumerRateCount,
                                   long connectionRateCount, long controllerRateCount, long requestPercentCount) {
    }

    public CompletableFuture<AllQuotasSummary> getAllQuotasSummary(String clusterId) {
        return getAll(clusterId)
                .thenApply(quotas ->
                        new AllQuotasSummary(
                                quotas.size(),
                                quotas.stream().filter(it -> it.getConfig().getInternalType() == Quota.ConfigType.PRODUCER_RATE).count(),
                                quotas.stream().filter(it -> it.getConfig().getInternalType() == Quota.ConfigType.CONSUMER_RATE).count(),
                                quotas.stream().filter(it -> it.getConfig().getInternalType() == Quota.ConfigType.CONNECTION_RATE).count(),
                                quotas.stream().filter(it -> it.getConfig().getInternalType() == Quota.ConfigType.CONTROLLER_RATE).count(),
                                quotas.stream().filter(it -> it.getConfig().getInternalType() == Quota.ConfigType.REQUEST_PERCENTAGE).count()
                        )
                );
    }
}
