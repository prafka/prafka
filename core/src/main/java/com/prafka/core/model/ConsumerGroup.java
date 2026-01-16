package com.prafka.core.model;

import lombok.Getter;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.GroupState;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public class ConsumerGroup {

    private final String id;
    private final GroupState state;
    private final Node coordinator;
    private final List<Member> members;
    private final Map<TopicPartition, Offset> partitionOffsets;
    private final Map<TopicPartition, Member> partitionMembers;

    public ConsumerGroup(ConsumerGroupDescription source,
                         Map<TopicPartition, OffsetAndMetadata> groupOffsetMap,
                         Map<TopicPartition, Long> beginOffsetMap,
                         Map<TopicPartition, Long> endOffsetMap) {
        id = source.groupId();
        state = source.groupState();
        coordinator = new Node(source.coordinator());
        members = source.members().stream().map(Member::new).toList();
        partitionOffsets = groupOffsetMap.entrySet().stream()
                .collect(
                        Collectors.toMap(Map.Entry::getKey,
                                entry -> new Offset(
                                        groupOffsetMap.get(entry.getKey()).offset(),
                                        beginOffsetMap.get(entry.getKey()),
                                        endOffsetMap.get(entry.getKey())
                                )
                        )
                );
        partitionMembers = members.stream()
                .flatMap(member -> member.getPartitions().stream().collect(Collectors.toMap(Function.identity(), tp -> member)).entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        partitionMembers.forEach((tp, member) -> partitionOffsets.computeIfAbsent(tp, key -> new Offset(0, 0, 0))); // todo provide other options
    }

    public Map<TopicPartition, Offset> getPartitionOffsets(String topic) {
        return partitionOffsets.entrySet().stream()
                .filter(it -> topic.equals(it.getKey().topic()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<TopicPartition, Offset> getPartitionOffset(String topic, int partition) {
        return partitionOffsets.entrySet().stream()
                .filter(it -> topic.equals(it.getKey().topic()) && partition == it.getKey().partition())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public List<String> getTopics() {
        return partitionOffsets.keySet().stream().map(TopicPartition::topic).distinct().toList();
    }

    public long getTopicLag(String topic) {
        return partitionOffsets.entrySet().stream()
                .collect(Collectors.groupingBy(entry -> entry.getKey().topic())).entrySet().stream()
                .filter(entry -> entry.getKey().equals(topic))
                .flatMap(entry -> entry.getValue().stream().map(offsets -> offsets.getValue().getLag()))
                .reduce(0L, Long::sum);
    }

    public long getOverallLag() {
        return partitionOffsets.values().stream().map(Offset::getLag).reduce(0L, Long::sum);
    }

    public Collection<String> getAssignedTopics() {
        return partitionMembers.keySet().stream().map(TopicPartition::topic).distinct().toList();
    }

    public Collection<TopicPartition> getPartitions(String topic) {
        return partitionOffsets.keySet().stream().filter(it -> it.topic().equals(topic)).toList();
    }

    public Collection<TopicPartition> getAssignedPartitions() {
        return partitionMembers.keySet();
    }

    public Collection<TopicPartition> getAssignedPartitions(String topic) {
        return partitionMembers.keySet().stream().filter(it -> it.topic().equals(topic)).toList();
    }

    public record GroupIdState(String groupId, GroupState state) {
    }

    public record Offset(long current, long begin, long end) {

        public long getLag() {
            return end - current;
        }
    }

    public enum OffsetStrategy {
        EARLIEST,
        LATEST,
        SPECIFIC,
        SHIFT,
        DATETIME,
        TIMESTAMP,
    }

    @Getter
    public static class Member {

        private final String consumerId;
        private final String groupInstanceId;
        private final String clientId;
        private final String host;
        private final Set<TopicPartition> partitions;

        public Member(MemberDescription source) {
            consumerId = source.consumerId();
            groupInstanceId = source.groupInstanceId().orElse(null);
            clientId = source.clientId();
            host = source.host();
            partitions = source.assignment().topicPartitions();
        }
    }
}
