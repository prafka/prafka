package com.prafka.core.manager;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import org.sourcelab.kafka.connect.apiclient.KafkaConnectClient;

import java.util.Map;
import java.util.Properties;

/**
 * Central factory interface for creating and managing Kafka client instances.
 *
 * <p>Provides methods to obtain Admin clients, Consumers, Producers, Schema Registry clients,
 * and Kafka Connect clients. Implementations typically cache client instances per cluster
 * to avoid recreating connections on each request.
 *
 * @see AbstractKafkaManager
 * @see Closeable
 */
public interface KafkaManager extends Closeable {

    Admin createAdminClient(Properties properties);

    Admin getAdminClient(String clusterId);

    Consumer<byte[], byte[]> getConsumer(String clusterId);

    Consumer<byte[], byte[]> getConsumer(String clusterId, Properties properties);

    Producer<byte[], byte[]> getProducer(String clusterId);

    Producer<byte[], byte[]> getProducer(String clusterId, Properties properties);

    boolean schemaRegistryIsDefined(String clusterId);

    RestService createSchemaRegistryRestService(Properties properties);

    SchemaRegistryClient getSchemaRegistryClient(String clusterId);

    boolean connectsIsDefined(String clusterId);

    KafkaConnectClient createConnectClient(Properties properties);

    KafkaConnectClient getConnectClient(String clusterId, String connectId);

    Map<String, KafkaConnectClient> getConnectClients(String clusterId);
}
