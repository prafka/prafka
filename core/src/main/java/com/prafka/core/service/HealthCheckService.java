package com.prafka.core.service;

import com.prafka.core.util.StreamUtils;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service for performing health checks on Kafka ecosystem components.
 *
 * <p>Checks the availability of the Kafka cluster, Schema Registry, and Kafka Connect
 * instances. Each check includes a timeout to prevent blocking on unresponsive services.
 */
@Named
@Singleton
public class HealthCheckService extends AbstractService {

    public record HealthCheckResult(Item cluster, Optional<Item> schemaRegistry, Optional<Map<String, Item>> connects) {
        public record Item(boolean available, Throwable error) {
        }
    }

    public CompletableFuture<HealthCheckResult> isAvailable(String clusterId) {
        var futures = new ArrayList<CompletableFuture<HealthCheckResult.Item>>();
        futures.add(clusterIsAvailable(clusterId));
        futures.add(schemaRegistryIsAvailable(clusterId));
        var connectIds = kafkaManager.connectsIsDefined(clusterId) ? connectClients(clusterId).keySet().stream().toList() : Collections.<String>emptyList();
        connectIds.forEach(it -> futures.add(connectIsAvailable(clusterId, it)));
        return futures.stream()
                .map(it -> it.thenApply(Collections::singletonList))
                .reduce(StreamUtils.combineFutureList())
                .orElseGet(StreamUtils.completeFutureEmptyList())
                .thenApply(list -> {
                    var connects = new HashMap<String, HealthCheckResult.Item>();
                    for (int i = 0; i < connectIds.size(); i++) {
                        connects.put(connectIds.get(i), list.get(i + 2));
                    }
                    return new HealthCheckResult(
                            list.get(0),
                            kafkaManager.schemaRegistryIsDefined(clusterId) ? Optional.of(list.get(1)) : Optional.empty(),
                            kafkaManager.connectsIsDefined(clusterId) ? Optional.of(connects) : Optional.empty()
                    );
                });
    }

    public CompletableFuture<HealthCheckResult.Item> clusterIsAvailable(String clusterId) {
        return adminClient(clusterId)
                .describeCluster()
                .clusterId()
                .toCompletionStage()
                .toCompletableFuture()
                .thenApply(it -> new HealthCheckResult.Item(true, null))
                .exceptionally(it -> new HealthCheckResult.Item(false, it));
    }

    public CompletableFuture<HealthCheckResult.Item> schemaRegistryIsAvailable(String clusterId) {
        return CompletableFuture.supplyAsync(() -> StreamUtils.tryReturn(() -> schemaRegistryClient(clusterId).getMode()))
                .thenApply(it -> new HealthCheckResult.Item(true, null))
                .exceptionally(it -> new HealthCheckResult.Item(false, it))
                .completeOnTimeout(new HealthCheckResult.Item(false, new TimeoutException()), 5000, TimeUnit.MILLISECONDS);
    }

    public CompletableFuture<HealthCheckResult.Item> connectIsAvailable(String clusterId, String connectId) {
        return CompletableFuture.supplyAsync(() -> connectClient(clusterId, connectId).getConnectServerVersion())
                .thenApply(it -> new HealthCheckResult.Item(true, null))
                .exceptionally(it -> new HealthCheckResult.Item(false, it))
                .completeOnTimeout(new HealthCheckResult.Item(false, new TimeoutException()), 5000, TimeUnit.MILLISECONDS);
    }
}
