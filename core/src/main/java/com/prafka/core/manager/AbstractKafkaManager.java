package com.prafka.core.manager;

import io.confluent.kafka.schemaregistry.SchemaProvider;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientFactory;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import io.confluent.kafka.schemaregistry.client.security.SslFactory;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.sourcelab.kafka.connect.apiclient.Configuration;
import org.sourcelab.kafka.connect.apiclient.KafkaConnectClient;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class AbstractKafkaManager implements KafkaManager {

    private final Map<String, Admin> adminClients = new ConcurrentHashMap<>();
    private final Map<String, Properties> consumerProperties = new ConcurrentHashMap<>();
    private final Map<String, Properties> producerProperties = new ConcurrentHashMap<>();
    private final Map<String, SchemaRegistryClient> schemaRegistryClients = new ConcurrentHashMap<>();
    private final Map<String, Map<String, KafkaConnectClient>> connectClients = new ConcurrentHashMap<>();

    @Override
    public Admin createAdminClient(Properties properties) {
        return Admin.create(properties);
    }

    @Override
    public Admin getAdminClient(String clusterId) {
        return adminClients.computeIfAbsent(clusterId, id -> createAdminClient(getAdminClientProperties(clusterId)));
    }

    @Override
    public Consumer<byte[], byte[]> getConsumer(String clusterId) {
        return getConsumer(clusterId, new Properties());
    }

    @Override
    public Consumer<byte[], byte[]> getConsumer(String clusterId, Properties additionalProperties) {
        var properties = consumerProperties.computeIfAbsent(clusterId, id -> getConsumerProperties(clusterId));
        properties.putAll(additionalProperties);
        return new KafkaConsumer<>(properties, new ByteArrayDeserializer(), new ByteArrayDeserializer());
    }

    @Override
    public Producer<byte[], byte[]> getProducer(String clusterId) {
        return getProducer(clusterId, new Properties());
    }

    @Override
    public Producer<byte[], byte[]> getProducer(String clusterId, Properties additionalProperties) {
        var properties = producerProperties.computeIfAbsent(clusterId, id -> getProducerProperties(clusterId));
        properties.putAll(additionalProperties);
        return new KafkaProducer<>(properties, new ByteArraySerializer(), new ByteArraySerializer());
    }

    @Override
    public RestService createSchemaRegistryRestService(Properties properties) {
        var url = properties.getProperty(SchemaRegistryClientConfig.CLIENT_NAMESPACE + "url");
        var restService = new RestService(url);
        var configs = toMap(properties).entrySet().stream()
                .map(entry -> {
                    var key = entry.getKey().startsWith(SchemaRegistryClientConfig.CLIENT_NAMESPACE)
                            ? entry.getKey().substring(SchemaRegistryClientConfig.CLIENT_NAMESPACE.length())
                            : entry.getKey();
                    return Map.entry(key, entry.getValue());
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        restService.configure(configs);
        var sslFactory = new SslFactory(configs);
        if (sslFactory.sslContext() != null) {
            restService.setSslSocketFactory(sslFactory.sslContext().getSocketFactory());
        }
        var sslEndpointIdentificationAlgo = (String) configs.get(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG);
        if (StringUtils.isBlank(sslEndpointIdentificationAlgo) || Strings.CI.equals(sslEndpointIdentificationAlgo, "none")) {
            restService.setHostnameVerifier((hostname, session) -> true);
        }
        return restService;
    }

    @Override
    public SchemaRegistryClient getSchemaRegistryClient(String clusterId) {
        return schemaRegistryClients.computeIfAbsent(clusterId, id -> {
            var properties = getSchemaRegistryClientProperties(clusterId);
            var url = properties.getProperty(SchemaRegistryClientConfig.CLIENT_NAMESPACE + "url");
            var providers = List.<SchemaProvider>of(new AvroSchemaProvider(), new JsonSchemaProvider(), new ProtobufSchemaProvider());
            return SchemaRegistryClientFactory.newClient(
                    Collections.singletonList(url),
                    256,
                    providers,
                    toMap(properties),
                    null
            );
        });
    }

    @Override
    public KafkaConnectClient createConnectClient(Properties properties) {
        var url = properties.getProperty("url");
        var configuration = new Configuration(url);
        if (properties.containsKey("basic.username") && properties.containsKey("basic.password")) {
            configuration.useBasicAuth(properties.getProperty("basic.username"), properties.getProperty("basic.password"));
        }
        if (properties.containsKey(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG)) {
            configuration.useKeyStore(new File(properties.getProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG)), properties.getProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG));
        }
        if (properties.containsKey(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG)) {
            configuration.useTrustStore(new File(properties.getProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG)), properties.getProperty(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG));
        }
        var sslEndpointIdentificationAlgo = properties.getProperty(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG);
        if (StringUtils.isBlank(sslEndpointIdentificationAlgo) || Strings.CI.equals(sslEndpointIdentificationAlgo, "none")) {
            configuration.useInsecureSslCertificates();
        }
        return new KafkaConnectClient(configuration);
    }

    @Override
    public KafkaConnectClient getConnectClient(String clusterId, String connectId) {
        return connectClients.computeIfAbsent(clusterId, id -> new ConcurrentHashMap<>()).computeIfAbsent(connectId, id ->
                createConnectClient(getConnectClientProperties(clusterId, connectId))
        );
    }

    @Override
    public Map<String, KafkaConnectClient> getConnectClients(String clusterId) {
        return getConnectIds(clusterId).stream()
                .map(connectId -> Map.entry(connectId, getConnectClient(clusterId, connectId)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    protected abstract Properties getAdminClientProperties(String clusterId);

    protected abstract Properties getConsumerProperties(String clusterId);

    protected abstract Properties getProducerProperties(String clusterId);

    protected abstract Properties getSchemaRegistryClientProperties(String clusterId);

    protected abstract Properties getConnectClientProperties(String clusterId, String connectId);

    protected abstract List<String> getConnectIds(String clusterId);

    private static Map<String, Object> toMap(Properties properties) {
        var map = new HashMap<String, Object>();
        properties.forEach((k, v) -> map.put((String) k, v));
        return map;
    }

    @Override
    public void close() {
        adminClients.values().forEach(it -> it.close(Duration.ofMillis(5000)));
        adminClients.clear();
        consumerProperties.clear();
        producerProperties.clear();
        schemaRegistryClients.values().forEach(SchemaRegistryClient::reset);
        schemaRegistryClients.clear();
        connectClients.clear();
    }

    @Override
    public void close(String clusterId) {
        Optional.ofNullable(adminClients.remove(clusterId)).ifPresent(it -> it.close(Duration.ofMillis(5000)));
        consumerProperties.remove(clusterId);
        producerProperties.remove(clusterId);
        Optional.ofNullable(schemaRegistryClients.remove(clusterId)).ifPresent(SchemaRegistryClient::reset);
        connectClients.remove(clusterId);
    }
}
