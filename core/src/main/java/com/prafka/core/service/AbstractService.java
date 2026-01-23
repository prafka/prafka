package com.prafka.core.service;

import com.prafka.core.manager.KafkaManager;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import jakarta.inject.Inject;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sourcelab.kafka.connect.apiclient.KafkaConnectClient;

import java.util.Map;
import java.util.Properties;

/**
 * Abstract base class for all Kafka-related services.
 *
 * <p>Provides convenient access methods to Kafka clients (Admin, Consumer, Producer),
 * Schema Registry client, and Kafka Connect clients through the injected {@link KafkaManager}.
 *
 * @see KafkaManager
 */
public abstract class AbstractService {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected KafkaManager kafkaManager;

    @Inject
    public void setKafkaManager(KafkaManager kafkaManager) {
        this.kafkaManager = kafkaManager;
    }

    protected Admin adminClient(String clusterId) {
        return kafkaManager.getAdminClient(clusterId);
    }

    protected Consumer<byte[], byte[]> consumer(String clusterId) {
        return kafkaManager.getConsumer(clusterId);
    }

    protected Consumer<byte[], byte[]> consumer(String clusterId, Properties properties) {
        return kafkaManager.getConsumer(clusterId, properties);
    }

    protected Producer<byte[], byte[]> producer(String clusterId) {
        return kafkaManager.getProducer(clusterId);
    }

    protected Producer<byte[], byte[]> producer(String clusterId, Properties properties) {
        return kafkaManager.getProducer(clusterId, properties);
    }

    protected SchemaRegistryClient schemaRegistryClient(String clusterId) {
        return kafkaManager.getSchemaRegistryClient(clusterId);
    }

    protected KafkaConnectClient connectClient(String clusterId, String connectId) {
        return kafkaManager.getConnectClient(clusterId, connectId);
    }

    protected Map<String, KafkaConnectClient> connectClients(String clusterId) {
        return kafkaManager.getConnectClients(clusterId);
    }

    protected void logDebugError(Throwable throwable) {
        if (log.isDebugEnabled()) {
            log.debug("Unknown error", throwable);
        }
    }
}
