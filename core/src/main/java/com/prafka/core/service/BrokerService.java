package com.prafka.core.service;

import com.prafka.core.model.Broker;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Named
@Singleton
public class BrokerService extends AbstractService {

    public CompletableFuture<List<Broker>> getAll(String clusterId) {
        var describeCluster = adminClient(clusterId).describeCluster();
        return describeCluster
                .controller()
                .toCompletionStage()
                .thenCompose(controllerNode ->
                        describeCluster
                                .nodes()
                                .toCompletionStage()
                                .thenApply(nodeList ->
                                        nodeList.stream()
                                                .map(node -> new Broker(node, controllerNode != null && node.id() == controllerNode.id()))
                                                .toList()
                                )
                )
                .toCompletableFuture();
    }

    public CompletableFuture<Broker> get(String clusterId, Integer brokerId) {
        return getAll(clusterId).thenApply(brokerList -> brokerList.stream().filter(broker -> broker.getId() == brokerId).findFirst().orElseThrow());
    }

    public record AllBrokersSummary(int brokerCount, boolean controller) {
    }

    public CompletableFuture<AllBrokersSummary> getAllBrokersSummary(String clusterId) {
        var describeCluster = adminClient(clusterId).describeCluster();
        return describeCluster
                .controller()
                .toCompletionStage()
                .thenCompose(controllerNode ->
                        describeCluster
                                .nodes()
                                .toCompletionStage()
                                .thenApply(nodeList -> new AllBrokersSummary(nodeList.size(), controllerNode != null))
                )
                .toCompletableFuture();
    }

    public record BrokerSummary(String host, int port, boolean controller) {
    }

    public CompletableFuture<BrokerSummary> getBrokerSummary(String clusterId, Integer brokerId) {
        return get(clusterId, brokerId).thenApply(broker -> new BrokerSummary(broker.getHost(), broker.getPort(), broker.isController()));
    }
}
