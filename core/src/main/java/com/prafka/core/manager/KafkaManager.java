package com.prafka.core.manager;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import org.sourcelab.kafka.connect.apiclient.KafkaConnectClient;

import java.util.Map;
import java.util.Properties;

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
