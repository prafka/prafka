package com.prafka.core.service;

import com.prafka.core.model.LogDir;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Named
@Singleton
public class LogDirService extends AbstractService {

    public CompletableFuture<Map<Integer, Map<TopicPartition, List<LogDir>>>> getAllByBrokers(String clusterId, Collection<Integer> brokerIds) {
        return adminClient(clusterId)
                .describeLogDirs(brokerIds)
                .allDescriptions()
                .toCompletionStage()
                .thenApply(brokerMap -> {
                    Map<Integer, Map<TopicPartition, List<LogDir>>> brokerToTopicMap = new HashMap<>();
                    for (var brokerEntry : brokerMap.entrySet()) {
                        Map<TopicPartition, List<LogDir>> topicToLogDirMap = new HashMap<>();
                        brokerToTopicMap.put(brokerEntry.getKey(), topicToLogDirMap);
                        for (var dirEntry : brokerEntry.getValue().entrySet()) {
                            for (var replicaEntry : dirEntry.getValue().replicaInfos().entrySet()) {
                                var logDir = new LogDir(brokerEntry.getKey(), dirEntry.getKey(), replicaEntry.getValue());
                                var logDirList = topicToLogDirMap.get(replicaEntry.getKey());
                                if (logDirList == null) {
                                    topicToLogDirMap.put(replicaEntry.getKey(), new ArrayList<>() {{
                                        add(logDir);
                                    }});
                                } else {
                                    logDirList.add(logDir);
                                }
                            }
                        }
                    }
                    return brokerToTopicMap;
                })
                .toCompletableFuture();
    }

    public CompletableFuture<Map<TopicPartition, List<LogDir>>> getAllByBroker(String clusterId, Integer brokerId) {
        return getAllByBrokers(clusterId, Collections.singletonList(brokerId))
                .thenApply(it -> it.getOrDefault(brokerId, Collections.emptyMap()));
    }

    public CompletableFuture<Map<String, Map<TopicPartition, List<LogDir>>>> getAllByTopics(String clusterId, Collection<String> topicNames) {
        return adminClient(clusterId)
                .describeCluster()
                .nodes()
                .toCompletionStage()
                .thenCompose(nodeList ->
                        getAllByBrokers(clusterId, nodeList.stream().map(Node::id).toList())
                                .thenApply(Map::values)
                                .thenApply(brokerToTopicMap -> {
                                    Map<TopicPartition, List<LogDir>> topicToLogDirMap = new HashMap<>();
                                    brokerToTopicMap.forEach(it -> it.forEach((key, value) -> topicToLogDirMap.merge(key, value, (v1, v2) -> {
                                        v1.addAll(v2);
                                        return v1;
                                    })));
                                    return topicToLogDirMap;
                                })
                )
                .thenApply(logDirMap ->
                        logDirMap.entrySet().stream()
                                .filter(it -> topicNames.contains(it.getKey().topic()))
                                .collect(Collectors.groupingBy(it -> it.getKey().topic(), Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                )
                .toCompletableFuture();
    }

    public CompletableFuture<Map<Integer, List<LogDir>>> getAllByTopic(String clusterId, String topicName) {
        return getAllByTopics(clusterId, Collections.singleton(topicName))
                .thenApply(it -> it.get(topicName))
                .thenApply(topicToLogDirMap -> topicToLogDirMap.entrySet().stream().collect(Collectors.toMap(it -> it.getKey().partition(), Map.Entry::getValue)));
    }

    public record AllTopicsSummary(long size) {
    }

    public CompletableFuture<AllTopicsSummary> getAllTopicsSummary(String clusterId) {
        return adminClient(clusterId)
                .listTopics(new ListTopicsOptions().listInternal(true))
                .names()
                .toCompletionStage()
                .thenCompose(it -> getAllByTopics(clusterId, it))
                .thenApply(Map::values)
                .thenApply(logDirMapList -> new AllTopicsSummary(
                        logDirMapList.stream().flatMap(it -> it.values().stream()).flatMap(Collection::stream).map(LogDir::getSize).reduce(0L, Long::sum)
                ))
                .toCompletableFuture();
    }

    public record TopicSummary(long size) {
    }

    public CompletableFuture<TopicSummary> getTopicSummary(String clusterId, String topicName) {
        return getAllByTopics(clusterId, Collections.singleton(topicName))
                .thenApply(it -> it.get(topicName))
                .thenApply(logDirMap -> new TopicSummary(
                        logDirMap.values().stream().flatMap(Collection::stream).map(LogDir::getSize).reduce(0L, Long::sum)
                ))
                .toCompletableFuture();
    }
}
