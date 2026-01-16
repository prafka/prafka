package com.prafka.core.service;

import com.prafka.core.model.ConsumerGroup;
import com.prafka.core.model.Node;
import com.prafka.core.util.StreamUtils;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsSpec;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.GroupState;
import org.apache.kafka.common.TopicPartition;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Named
@Singleton
public class ConsumerGroupService extends AbstractService {

    public CompletableFuture<Collection<ConsumerGroup>> getAll(String clusterId) {
        return adminClient(clusterId)
                .listConsumerGroups()
                .all()
                .toCompletionStage()
                .thenCompose(consumerGroupList -> getAll(clusterId, consumerGroupList.stream().map(ConsumerGroupListing::groupId).toList()).thenApply(Map::values))
                .toCompletableFuture();
    }

    public CompletableFuture<List<ConsumerGroup.GroupIdState>> getAllGroupIdsWithState(String clusterId) {
        return adminClient(clusterId)
                .listConsumerGroups()
                .all()
                .thenApply(list -> list.stream().map(it -> new ConsumerGroup.GroupIdState(it.groupId(), it.groupState().orElse(GroupState.UNKNOWN))).toList())
                .toCompletionStage()
                .toCompletableFuture();
    }

    public CompletableFuture<List<String>> getAllGroupIds(String clusterId) {
        return getAllGroupIdsWithState(clusterId).thenApply(list -> list.stream().map(ConsumerGroup.GroupIdState::groupId).toList());
    }

    public CompletableFuture<Map<String, ConsumerGroup>> getAll(String clusterId, List<String> groupIdList) {
        return adminClient(clusterId)
                .describeConsumerGroups(groupIdList)
                .describedGroups()
                .values().stream()
                .map(StreamUtils::mapKafkaFutureToList)
                .reduce(StreamUtils.combineFutureList())
                .orElseGet(StreamUtils.completeFutureEmptyList())
                .thenCompose(consumerGroupList -> {
                    var consumerGroupMap = consumerGroupList.stream().collect(Collectors.toMap(ConsumerGroupDescription::groupId, Function.identity()));
                    var listConsumerGroupOffsets = adminClient(clusterId)
                            .listConsumerGroupOffsets(consumerGroupMap.keySet().stream().collect(Collectors.toMap(Function.identity(), it -> new ListConsumerGroupOffsetsSpec())));
                    return consumerGroupMap.keySet().stream()
                            .map(groupId ->
                                    listConsumerGroupOffsets
                                            .partitionsToOffsetAndMetadata(groupId)
                                            .toCompletionStage()
                                            .thenApplyAsync(groupOffsetMap -> {
                                                try (var consumer = consumer(clusterId)) {
                                                    var beginOffsetMap = consumer.beginningOffsets(groupOffsetMap.keySet());
                                                    var endOffsetMap = consumer.endOffsets(groupOffsetMap.keySet());
                                                    var consumerGroup = new ConsumerGroup(consumerGroupMap.get(groupId), groupOffsetMap, beginOffsetMap, endOffsetMap);
                                                    return Collections.singletonList(consumerGroup);
                                                }
                                            })
                                            .exceptionally(e -> Collections.emptyList())
                                            .toCompletableFuture()
                            )
                            .reduce(StreamUtils.combineFutureList())
                            .orElseGet(StreamUtils.completeFutureEmptyList());
                })
                .thenApply(groupList -> groupList.stream()
                        .map(group -> Map.entry(group.getId(), group))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                );
    }

    public CompletableFuture<List<ConsumerGroup>> getAllByTopic(String clusterId, String topic) {
        return getAll(clusterId).thenApply(consumerGroupList -> consumerGroupList.stream().filter(it -> it.getTopics().contains(topic)).toList());
    }

    public CompletableFuture<ConsumerGroup> get(String clusterId, String groupId) {
        return getAll(clusterId, Collections.singletonList(groupId)).thenApply(it -> it.get(groupId));
    }

    public CompletableFuture<Void> create(String clusterId, String groupId, ConsumerGroup.OffsetStrategy strategy, List<String> topics) {
        return CompletableFuture.runAsync(() -> {
            var properties = new Properties();
            properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            switch (strategy) {
                case EARLIEST -> properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
                case LATEST -> properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
                default -> throw new IllegalArgumentException();
            }
            try (var consumer = consumer(clusterId, properties)) {
                var topicPartitionList = topics.stream().flatMap(it -> consumer.partitionsFor(it).stream()).map(it -> new TopicPartition(it.topic(), it.partition())).toList();
                consumer.assign(topicPartitionList);
                var offsetMap = switch (strategy) {
                    case EARLIEST -> consumer.beginningOffsets(topicPartitionList);
                    case LATEST -> consumer.endOffsets(topicPartitionList);
                    default -> throw new IllegalArgumentException();
                };
                consumer.commitSync(offsetMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, it -> new OffsetAndMetadata(it.getValue()))));
            }
        });
    }

    public record CalculateNewOffsetsFilter(ConsumerGroup.OffsetStrategy strategy, Optional<Integer> specific,
                                            Optional<Integer> shift, Optional<Long> timestamp) {
    }

    public CompletableFuture<Map<TopicPartition, Long>> calculateNewOffsets(String clusterId, String groupId, Map<TopicPartition, ConsumerGroup.Offset> offsets, CalculateNewOffsetsFilter filter) {
        if (filter.strategy() == ConsumerGroup.OffsetStrategy.EARLIEST) {
            return CompletableFuture.completedFuture(
                    offsets.entrySet().stream()
                            .map(entry -> {
                                long newOffset = entry.getValue().begin();
                                return Map.entry(entry.getKey(), newOffset);
                            })
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            );
        }
        if (filter.strategy() == ConsumerGroup.OffsetStrategy.LATEST) {
            return CompletableFuture.completedFuture(
                    offsets.entrySet().stream()
                            .map(entry -> {
                                long newOffset = entry.getValue().end();
                                return Map.entry(entry.getKey(), newOffset);
                            })
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            );
        }
        if (filter.strategy() == ConsumerGroup.OffsetStrategy.SPECIFIC && filter.specific().isPresent()) {
            return CompletableFuture.completedFuture(
                    offsets.entrySet().stream()
                            .map(entry -> {
                                long newOffset;
                                if (filter.specific().get() >= entry.getValue().begin() && filter.specific().get() <= entry.getValue().end()) {
                                    newOffset = filter.specific().get();
                                } else {
                                    newOffset = entry.getValue().current();
                                }
                                return Map.entry(entry.getKey(), newOffset);
                            })
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            );
        }
        if (filter.strategy() == ConsumerGroup.OffsetStrategy.SHIFT && filter.shift().isPresent()) {
            return CompletableFuture.completedFuture(
                    offsets.entrySet().stream()
                            .map(entry -> {
                                long newOffset = Math.min(entry.getValue().current() + filter.shift().get(), entry.getValue().end());
                                return Map.entry(entry.getKey(), newOffset);
                            })
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            );
        }
        if ((filter.strategy() == ConsumerGroup.OffsetStrategy.DATETIME || filter.strategy() == ConsumerGroup.OffsetStrategy.TIMESTAMP) && filter.timestamp().isPresent()) {
            return CompletableFuture.supplyAsync(() -> {
                try (var consumer = consumer(clusterId)) {
                    var partitionTimestampMap = offsets.keySet().stream()
                            .collect(Collectors.toMap(Function.identity(), it -> filter.timestamp().get()));
                    return consumer.offsetsForTimes(partitionTimestampMap).entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, it -> (it.getValue() != null) ? it.getValue().offset() : offsets.get(it.getKey()).current()));
                }
            });
        }
        throw new IllegalStateException();
    }

    public CompletableFuture<Void> updateOffsets(String clusterId, String groupId, Map<TopicPartition, Long> offsets) {
        return CompletableFuture.runAsync(() -> {
            var properties = new Properties();
            properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            try (var consumer = consumer(clusterId, properties)) {
                consumer.commitSync(offsets.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, it -> new OffsetAndMetadata(it.getValue()))));
            }
        });
    }

    public CompletableFuture<Void> delete(String clusterId, String groupId) {
        return adminClient(clusterId)
                .deleteConsumerGroups(Collections.singletonList(groupId))
                .all()
                .toCompletionStage()
                .toCompletableFuture();
    }

    public record AllGroupsSummary(int groupCount, long stableCount, long preparingRebalanceCount,
                                   long completingRebalanceCount, long emptyCount, long deadCount) {
    }

    public CompletableFuture<AllGroupsSummary> getAllGroupsSummary(String clusterId) {
        return adminClient(clusterId)
                .listConsumerGroups()
                .all()
                .thenApply(list -> new AllGroupsSummary(
                        list.size(),
                        list.stream().filter(it -> it.groupState().orElse(GroupState.UNKNOWN) == GroupState.STABLE).count(),
                        list.stream().filter(it -> it.groupState().orElse(GroupState.UNKNOWN) == GroupState.PREPARING_REBALANCE).count(),
                        list.stream().filter(it -> it.groupState().orElse(GroupState.UNKNOWN) == GroupState.COMPLETING_REBALANCE).count(),
                        list.stream().filter(it -> it.groupState().orElse(GroupState.UNKNOWN) == GroupState.EMPTY).count(),
                        list.stream().filter(it -> it.groupState().orElse(GroupState.UNKNOWN) == GroupState.DEAD).count()
                ))
                .toCompletionStage()
                .toCompletableFuture();
    }

    public record GroupSummary(GroupState state, long memberCount, Node coordinator, long topicCount,
                               long assignedTopicCount, long partitionCount, long assignedPartitionCount,
                               long overallLag) {
    }

    public CompletableFuture<GroupSummary> getGroupSummary(String clusterId, String groupId) {
        return get(clusterId, groupId)
                .thenApply(group -> new GroupSummary(
                        group.getState(),
                        group.getMembers().size(),
                        group.getCoordinator(),
                        group.getTopics().size(),
                        group.getAssignedTopics().size(),
                        group.getPartitionOffsets().size(),
                        group.getAssignedPartitions().size(),
                        group.getOverallLag()
                ));
    }
}
