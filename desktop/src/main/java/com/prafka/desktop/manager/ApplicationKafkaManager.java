package com.prafka.desktop.manager;

import com.prafka.core.manager.AbstractKafkaManager;
import com.prafka.desktop.model.ClusterModel;
import com.prafka.desktop.service.ConnectionPropertiesService;
import com.prafka.desktop.service.SessionService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.Strings;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.util.List;
import java.util.Properties;

@Singleton
public class ApplicationKafkaManager extends AbstractKafkaManager {

    private final SessionService sessionService;
    private final ConnectionPropertiesService connectionPropertiesService;

    @Inject
    public ApplicationKafkaManager(SessionService sessionService, ConnectionPropertiesService connectionPropertiesService) {
        this.sessionService = sessionService;
        this.connectionPropertiesService = connectionPropertiesService;
    }

    @Override
    protected Properties getAdminClientProperties(String clusterId) {
        return new Properties() {{
            putAll(connectionPropertiesService.getKafkaProperties(sessionService.getCluster()));
        }};
    }

    @Override
    protected Properties getConsumerProperties(String clusterId) {
        return new Properties() {{
            putAll(connectionPropertiesService.getKafkaProperties(sessionService.getCluster()));
            put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        }};
    }

    @Override
    protected Properties getProducerProperties(String clusterId) {
        return new Properties() {{
            putAll(connectionPropertiesService.getKafkaProperties(sessionService.getCluster()));
        }};
    }

    @Override
    public boolean schemaRegistryIsDefined(String clusterId) {
        return sessionService.getCluster().isSchemaRegistryDefined();
    }

    @Override
    protected Properties getSchemaRegistryClientProperties(String clusterId) {
        return connectionPropertiesService.getSchemaRegistryProperties(sessionService.getCluster().getSchemaRegistry());
    }

    @Override
    public boolean connectsIsDefined(String clusterId) {
        return sessionService.getCluster().isConnectsDefined();
    }

    @Override
    protected Properties getConnectClientProperties(String clusterId, String connectId) {
        return connectionPropertiesService.getConnectProperties(
                sessionService.getCluster().getConnects().stream().filter(it -> Strings.CS.equals(it.getId(), connectId)).findFirst().orElseThrow()
        );
    }

    @Override
    protected List<String> getConnectIds(String s) {
        return sessionService.getCluster().getConnects().stream().map(ClusterModel.ConnectModel::getId).toList();
    }
}
