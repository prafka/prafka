package com.prafka.core.service;

import com.prafka.core.model.Topic;
import com.prafka.core.util.StreamUtils;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.RecordsToDelete;
import org.apache.kafka.common.TopicPartition;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for managing Kafka topics.
 *
 * <p>Provides operations to list, create, empty (delete all records), and delete topics.
 * Includes methods to retrieve topic details with partition information and offset ranges.
 *
 * @see Topic
 */
@Named
@Singleton
public class TopicService extends AbstractService {

    public CompletableFuture<Set<String>> getAllNames(String clusterId) {
        return adminClient(clusterId)
                .listTopics(new ListTopicsOptions().listInternal(true))
                .names()
                .toCompletionStage()
                .toCompletableFuture();
    }

    public CompletableFuture<Map<String, Topic>> getAll(String clusterId, Collection<String> topicNameList) {
        return adminClient(clusterId)
                .describeTopics(topicNameList)
                .topicNameValues()
                .values().stream()
                .map(StreamUtils::mapKafkaFutureToList)
                .reduce(StreamUtils.combineFutureList())
                .orElseGet(StreamUtils.completeFutureEmptyList())
                .thenApplyAsync(topicDescriptionList -> {
                    var topicPartitionList = topicDescriptionList.stream()
                            .flatMap(topicDescription ->
                                    topicDescription.partitions().stream()
                                            .map(topicPartitionInfo -> new TopicPartition(topicDescription.name(), topicPartitionInfo.partition()))
                            )
                            .toList();
                    try (var consumer = consumer(clusterId)) {
                        var beginOffsetMap = consumer.beginningOffsets(topicPartitionList);
                        var endOffsetMap = consumer.endOffsets(topicPartitionList);
                        return topicDescriptionList.stream()
                                .map(topicDescription -> Map.entry(topicDescription.name(), new Topic(topicDescription, beginOffsetMap, endOffsetMap)))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    }
                });
    }

    public CompletableFuture<Topic> get(String clusterId, String topicName) {
        return getAll(clusterId, Collections.singletonList(topicName)).thenApply(it -> it.get(topicName));
    }

    public CompletableFuture<Topic> create(String clusterId, String name, int partitions, short replicationFactor, Map<String, String> configs) {
        var newTopic = new NewTopic(name, partitions, replicationFactor);
        newTopic.configs(configs);
        return adminClient(clusterId)
                .createTopics(List.of(newTopic))
                .all()
                .toCompletionStage()
                .thenCompose(it -> get(clusterId, name))
                .toCompletableFuture();
    }

    public CompletableFuture<Void> empty(String clusterId, String topicName) {
        return empty(clusterId, Collections.singletonList(topicName));
    }

    public CompletableFuture<Void> empty(String clusterId, Collection<String> topicNameList) {
        return getAll(clusterId, topicNameList)
                .thenApply(Map::values)
                .thenCompose(topics -> {
                    var recordToDeleteList = topics.stream()
                            .flatMap(topic -> topic.getPartitions().stream().map(it -> Map.entry(it.getTp(), RecordsToDelete.beforeOffset(it.getEndOffset()))))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    return adminClient(clusterId)
                            .deleteRecords(recordToDeleteList)
                            .all()
                            .toCompletionStage();
                });
    }

    public CompletableFuture<Void> delete(String clusterId, String topicName) {
        return delete(clusterId, Collections.singletonList(topicName));
    }

    public CompletableFuture<Void> delete(String clusterId, Collection<String> topicNameList) {
        return adminClient(clusterId)
                .deleteTopics(topicNameList)
                .all()
                .toCompletionStage()
                .toCompletableFuture();
    }

    public record AllTopicsSummary(int topicCount, int partitionCount, long recordCount) {
    }

    public CompletableFuture<AllTopicsSummary> getAllTopicsSummary(String clusterId) {
        return getAllNames(clusterId)
                .thenCompose(it -> getAll(clusterId, it))
                .thenApply(Map::values)
                .thenApply(topicList ->
                        new AllTopicsSummary(
                                topicList.size(),
                                topicList.stream().mapToInt(it -> it.getPartitions().size()).sum(),
                                topicList.stream().mapToLong(Topic::getRecordCount).sum()
                        )
                );
    }

    public record TopicSummary(int partitionCount, int replicationFactor, long recordCount) {
    }

    public CompletableFuture<TopicSummary> getTopicSummary(String clusterId, String topicName) {
        return get(clusterId, topicName)
                .thenApply(topic -> new TopicSummary(
                        topic.getPartitions().size(),
                        topic.getReplicaCount(),
                        topic.getRecordCount()
                ));
    }
}
