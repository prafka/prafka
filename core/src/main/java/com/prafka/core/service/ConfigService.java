package com.prafka.core.service;

import com.prafka.core.model.Config;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.DescribeConfigsOptions;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.record.CompressionType;
import org.apache.kafka.common.record.Records;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Named
@Singleton
public class ConfigService extends AbstractService {

    public CompletableFuture<List<Config>> getAllByBroker(String clusterId, Integer brokerId) {
        return getAllByType(clusterId, brokerId.toString(), ConfigResource.Type.BROKER);
    }

    public CompletableFuture<List<Config>> getAllByController(String clusterId) {
        var describeCluster = adminClient(clusterId).describeCluster();
        return describeCluster
                .controller()
                .toCompletionStage()
                .toCompletableFuture()
                .thenCompose(controllerNode -> {
                    if (controllerNode != null) {
                        return CompletableFuture.completedStage(controllerNode.id());
                    } else {
                        return describeCluster.nodes().toCompletionStage().thenApply(it -> it.stream().findFirst().orElseThrow().id());
                    }
                })
                .thenCompose(brokerId -> getAllByBroker(clusterId, brokerId));
    }

    public CompletableFuture<List<Config>> getAllByTopic(String clusterId, String topicName) {
        return getAllByType(clusterId, topicName, ConfigResource.Type.TOPIC);
    }

    private CompletableFuture<List<Config>> getAllByType(String clusterId, String name, ConfigResource.Type type) {
        return adminClient(clusterId)
                .describeConfigs(
                        Collections.singletonList(new ConfigResource(type, name)),
                        new DescribeConfigsOptions().includeSynonyms(true).includeDocumentation(true)
                )
                .all()
                .thenApply(it -> it.values().stream().findFirst().map(org.apache.kafka.clients.admin.Config::entries).orElseGet(Collections::emptyList))
                .thenApply(configList -> configList.stream().map(Config::new).toList())
                .toCompletionStage()
                .toCompletableFuture();
    }

    public CompletableFuture<Void> setByTopic(String clusterId, String topicName, String configName, String configValue) {
        return alterByTopic(clusterId, topicName, AlterConfigOp.OpType.SET, configName, configValue);
    }

    public CompletableFuture<Void> resetByTopic(String clusterId, String topicName, String configName) {
        return alterByTopic(clusterId, topicName, AlterConfigOp.OpType.DELETE, configName, null);
    }

    public CompletableFuture<Config> getDefaultReplicationFactor(String clusterId) {
        return getAllByController(clusterId)
                .thenApply(it -> it.stream().filter(config -> "default.replication.factor".equals(config.getName())).findFirst().orElseThrow());
    }

    public record AllBrokersSummary(String version) {
    }

    public CompletableFuture<AllBrokersSummary> getAllBrokersSummary(String clusterId) {
        return getAllByController(clusterId)
                .thenApply(it -> it.stream().filter(config -> "inter.broker.protocol.version".equals(config.getName())).findFirst().orElseThrow())
                .thenApply(it -> new AllBrokersSummary(it.getValue()));
    }

    public record TopicSummary(String cleanupPolicy) {
    }

    public CompletableFuture<TopicSummary> getTopicSummary(String clusterId, String topicName) {
        return getAllByTopic(clusterId, topicName)
                .thenApply(configList -> new TopicSummary(
                        configList.stream().filter(it -> TopicConfig.CLEANUP_POLICY_CONFIG.equals(it.getName())).map(Config::getValue).findFirst().orElseThrow()
                ));
    }

    private CompletableFuture<Void> alterByTopic(String clusterId, String topicName, AlterConfigOp.OpType opType, String configName, String configValue) {
        var configResource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
        var configEntry = new ConfigEntry(configName, configValue);
        var op = new AlterConfigOp(configEntry, opType);
        return adminClient(clusterId)
                .incrementalAlterConfigs(Map.of(configResource, List.of(op)))
                .all()
                .toCompletionStage()
                .toCompletableFuture();
    }

    // https://github.com/apache/kafka/blob/4.0.1/storage/src/main/java/org/apache/kafka/storage/internals/log/LogConfig.java
    private static final Map<String, DefaultConfig> TOPIC_DEFAULT_CONFIGS = Stream.of(
            new DefaultConfig(TopicConfig.SEGMENT_BYTES_CONFIG, 1024 * 1024 * 1024, TopicConfig.SEGMENT_BYTES_DOC),
            new DefaultConfig(TopicConfig.SEGMENT_MS_CONFIG, 24 * 7 * 60 * 60 * 1000L, TopicConfig.SEGMENT_MS_DOC),
            new DefaultConfig(TopicConfig.SEGMENT_JITTER_MS_CONFIG, 0, TopicConfig.SEGMENT_JITTER_MS_DOC),
            new DefaultConfig(TopicConfig.SEGMENT_INDEX_BYTES_CONFIG, 10 * 1024 * 1024, TopicConfig.SEGMENT_INDEX_BYTES_DOC),
            new DefaultConfig(TopicConfig.FLUSH_MESSAGES_INTERVAL_CONFIG, Long.MAX_VALUE, TopicConfig.FLUSH_MESSAGES_INTERVAL_DOC),
            new DefaultConfig(TopicConfig.FLUSH_MS_CONFIG, Long.MAX_VALUE, TopicConfig.FLUSH_MS_DOC),
            new DefaultConfig(TopicConfig.RETENTION_BYTES_CONFIG, -1L, TopicConfig.RETENTION_BYTES_DOC),
            new DefaultConfig(TopicConfig.RETENTION_MS_CONFIG, 24 * 7 * 60 * 60 * 1000L, TopicConfig.RETENTION_MS_DOC),
            new DefaultConfig(TopicConfig.MAX_MESSAGE_BYTES_CONFIG, 1024 * 1024 + Records.LOG_OVERHEAD, TopicConfig.MAX_MESSAGE_BYTES_DOC),
            new DefaultConfig(TopicConfig.INDEX_INTERVAL_BYTES_CONFIG, 4096, TopicConfig.INDEX_INTERVAL_BYTES_DOC),
            new DefaultConfig(TopicConfig.DELETE_RETENTION_MS_CONFIG, 24 * 60 * 60 * 1000L, TopicConfig.DELETE_RETENTION_MS_DOC),
            new DefaultConfig(TopicConfig.MIN_COMPACTION_LAG_MS_CONFIG, 0, TopicConfig.MIN_COMPACTION_LAG_MS_DOC),
            new DefaultConfig(TopicConfig.MAX_COMPACTION_LAG_MS_CONFIG, Long.MAX_VALUE, TopicConfig.MAX_COMPACTION_LAG_MS_DOC),
            new DefaultConfig(TopicConfig.FILE_DELETE_DELAY_MS_CONFIG, 60000L, TopicConfig.FILE_DELETE_DELAY_MS_DOC),
            new DefaultConfig(TopicConfig.MIN_CLEANABLE_DIRTY_RATIO_CONFIG, 0.5, TopicConfig.MIN_CLEANABLE_DIRTY_RATIO_DOC),
            new DefaultConfig(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE, TopicConfig.CLEANUP_POLICY_DOC),
            new DefaultConfig(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, 1, TopicConfig.MIN_IN_SYNC_REPLICAS_DOC),
            new DefaultConfig(TopicConfig.COMPRESSION_TYPE_CONFIG, "producer", TopicConfig.COMPRESSION_TYPE_DOC),
            new DefaultConfig(TopicConfig.COMPRESSION_GZIP_LEVEL_CONFIG, CompressionType.GZIP.defaultLevel(), TopicConfig.COMPRESSION_GZIP_LEVEL_DOC),
            new DefaultConfig(TopicConfig.COMPRESSION_LZ4_LEVEL_CONFIG, CompressionType.LZ4.defaultLevel(), TopicConfig.COMPRESSION_LZ4_LEVEL_DOC),
            new DefaultConfig(TopicConfig.COMPRESSION_ZSTD_LEVEL_CONFIG, CompressionType.ZSTD.defaultLevel(), TopicConfig.COMPRESSION_ZSTD_LEVEL_DOC),
            new DefaultConfig(TopicConfig.UNCLEAN_LEADER_ELECTION_ENABLE_CONFIG, false, TopicConfig.UNCLEAN_LEADER_ELECTION_ENABLE_DOC),
            new DefaultConfig(TopicConfig.PREALLOCATE_CONFIG, false, TopicConfig.PREALLOCATE_DOC),
            new DefaultConfig(TopicConfig.MESSAGE_TIMESTAMP_TYPE_CONFIG, "CreateTime", TopicConfig.MESSAGE_TIMESTAMP_TYPE_DOC),
            new DefaultConfig(TopicConfig.MESSAGE_TIMESTAMP_BEFORE_MAX_MS_CONFIG, Long.MAX_VALUE, TopicConfig.MESSAGE_TIMESTAMP_BEFORE_MAX_MS_DOC),
            new DefaultConfig(TopicConfig.MESSAGE_TIMESTAMP_AFTER_MAX_MS_CONFIG, 3600000, TopicConfig.MESSAGE_TIMESTAMP_AFTER_MAX_MS_DOC),
            new DefaultConfig(TopicConfig.LOCAL_LOG_RETENTION_MS_CONFIG, -2, TopicConfig.LOCAL_LOG_RETENTION_MS_DOC),
            new DefaultConfig(TopicConfig.LOCAL_LOG_RETENTION_BYTES_CONFIG, -2, TopicConfig.LOCAL_LOG_RETENTION_BYTES_DOC)
    ).collect(Collectors.toMap(DefaultConfig::name, Function.identity()));

    public Collection<DefaultConfig> getTopicDefaultConfigs() {
        return TOPIC_DEFAULT_CONFIGS.values();
    }

    public record DefaultConfig(String name, Object value, String documentation) {
    }
}
