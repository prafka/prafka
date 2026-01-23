package com.prafka.core.model;

import lombok.Getter;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;

import java.util.List;
import java.util.Map;

/**
 * Represents a Kafka topic with its partitions and metadata.
 *
 * <p>Contains topic information including the unique ID, name, partition details
 * (with offset ranges and replica assignments), and whether the topic is internal.
 *
 * @see TopicDescription
 */
@Getter
public class Topic {

    private final String id;
    private final String name;
    private final List<Partition> partitions;
    private final boolean internal;

    public Topic(TopicDescription source, Map<TopicPartition, Long> beginOffsetMap, Map<TopicPartition, Long> endOffsetMap) {
        id = source.topicId().toString();
        name = source.name();
        partitions = source.partitions().stream()
                .map(partitionInfo -> {
                    var tp = new TopicPartition(name, partitionInfo.partition());
                    return new Partition(partitionInfo, tp, beginOffsetMap.get(tp), endOffsetMap.get(tp));
                })
                .toList();
        internal = source.isInternal();
    }

    public long getRecordCount() {
        return getPartitions().stream().mapToLong(it -> it.getEndOffset() - it.getBeginOffset()).sum();
    }

    public int getReplicaCount() {
        return getPartitions().stream().mapToInt(it -> it.getReplicas().size()).max().orElse(0);
    }

    public int getInSyncReplicaCount() {
        return getPartitions().stream().mapToInt(partition -> (int) partition.getReplicas().stream().filter(Partition.Replica::isInSync).count()).min().orElse(0);
    }

    @Getter
    public static class Partition {

        private final int id;
        private final TopicPartition tp;
        private final List<Replica> replicas;
        private final long beginOffset;
        private final long endOffset;

        public Partition(TopicPartitionInfo source, TopicPartition tp, long beginOffset, long endOffset) {
            id = source.partition();
            this.tp = tp;
            replicas = source.replicas().stream()
                    .map(node -> {
                        var leader = source.leader() != null && source.leader().id() == node.id();
                        var inSync = source.isr().stream().anyMatch(it -> it.id() == node.id());
                        return new Replica(node, leader, inSync);
                    })
                    .toList();
            this.beginOffset = beginOffset;
            this.endOffset = endOffset;
        }

        @Getter
        public static class Replica extends Node {

            private final boolean leader;
            private final boolean inSync;

            public Replica(org.apache.kafka.common.Node source, boolean leader, boolean inSync) {
                super(source);
                this.leader = leader;
                this.inSync = inSync;
            }
        }
    }
}
