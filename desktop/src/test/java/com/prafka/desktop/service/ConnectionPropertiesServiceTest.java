package com.prafka.desktop.service;

import com.prafka.core.connection.AuthenticationMethod;
import com.prafka.desktop.model.ClusterModel;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionPropertiesServiceTest {

    private ConnectionPropertiesService connectionPropertiesService = new ConnectionPropertiesService();

    @Test
    void shouldGetKafkaProperties() {
        // Given
        var cluster = new ClusterModel();
        cluster.setBootstrapServers("localhost:9092");
        cluster.setAuthenticationMethod(AuthenticationMethod.SSL);
        var sslAuthentication = new ClusterModel.SslAuthentication();
        sslAuthentication.setTruststoreLocation("/path/to/truststore");
        cluster.setSslAuthentication(sslAuthentication);

        // When
        var result = connectionPropertiesService.getKafkaProperties(cluster);

        // Then
        assertEquals(cluster.getBootstrapServers(), result.getProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals(sslAuthentication.getTruststoreLocation(), result.getProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG));
    }

    @Test
    void shouldSchemaRegistryProperties() {
        // Given
        var schemaRegistry = new ClusterModel.SchemaRegistryModel();
        schemaRegistry.setUrl("http://localhost:8081");
        schemaRegistry.setAuthenticationMethod(AuthenticationMethod.TOKEN);
        var tokenAuthentication = new ClusterModel.TokenAuthentication();
        tokenAuthentication.setToken("test-token".toCharArray());
        schemaRegistry.setTokenAuthentication(tokenAuthentication);

        // When
        var result = connectionPropertiesService.getSchemaRegistryProperties(schemaRegistry);

        // Then
        assertEquals(schemaRegistry.getUrl(), result.getProperty(SchemaRegistryClientConfig.CLIENT_NAMESPACE + "url"));
        assertEquals(new String(tokenAuthentication.getToken()), result.getProperty(SchemaRegistryClientConfig.CLIENT_NAMESPACE + SchemaRegistryClientConfig.BEARER_AUTH_TOKEN_CONFIG));
    }

    @Test
    void shouldGetConnectPropertiesWithBasicAuthentication() {
        // Given
        var connect = new ClusterModel.ConnectModel();
        connect.setUrl("http://localhost:8083");
        connect.setAuthenticationMethod(AuthenticationMethod.BASIC);
        var basicAuthentication = new ClusterModel.BasicAuthentication();
        basicAuthentication.setUsername("test-user");
        basicAuthentication.setPassword("test-password".toCharArray());
        connect.setBasicAuthentication(basicAuthentication);

        // When
        var result = connectionPropertiesService.getConnectProperties(connect);

        // Then
        assertEquals(connect.getUrl(), result.getProperty("url"));
        assertEquals(basicAuthentication.getUsername(), result.getProperty("basic.username"));
        assertEquals(new String(basicAuthentication.getPassword()), result.getProperty("basic.password"));
    }

}