package com.prafka.core.manager;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientFactory;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sourcelab.kafka.connect.apiclient.KafkaConnectClient;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AbstractKafkaManagerTest {

    private AbstractKafkaManager kafkaManager;

    @BeforeEach
    void setUp() {
        kafkaManager = new KafkaManager();
    }

    @Test
    void shouldCreateAdminClient() {
        try (var mockStaticAdmin = mockStatic(Admin.class)) {
            // Given
            var clusterId = "test-cluster";
            var mockAdmin = mock(Admin.class);
            mockStaticAdmin.when(() -> Admin.create(any(Properties.class))).thenReturn(mockAdmin);

            // When
            var result = kafkaManager.getAdminClient(clusterId);

            // Then
            assertEquals(mockAdmin, result);
            mockStaticAdmin.verify(() -> Admin.create(any(Properties.class)));
        }
    }

    @Test
    void shouldReturnSameAdminClientForSameClusterId() {
        try (var mockStaticAdmin = mockStatic(Admin.class)) {
            // Given
            var clusterId = "test-cluster";
            var mockAdmin = mock(Admin.class);
            mockStaticAdmin.when(() -> Admin.create(any(Properties.class))).thenReturn(mockAdmin);

            // When
            var result1 = kafkaManager.getAdminClient(clusterId);
            var result2 = kafkaManager.getAdminClient(clusterId);

            // Then
            assertSame(result1, result2);
            mockStaticAdmin.verify(() -> Admin.create(any(Properties.class)));
        }
    }

    @Test
    void shouldCreateDifferentAdminClientForDifferentClusterId() {
        try (var mockStaticAdmin = mockStatic(Admin.class)) {
            // Given
            var clusterId1 = "test-cluster-1";
            var clusterId2 = "test-cluster-2";
            var mockAdmin1 = mock(Admin.class);
            var mockAdmin2 = mock(Admin.class);
            mockStaticAdmin.when(() -> Admin.create(any(Properties.class))).thenReturn(mockAdmin1).thenReturn(mockAdmin2);

            // When
            var result1 = kafkaManager.getAdminClient(clusterId1);
            var result2 = kafkaManager.getAdminClient(clusterId2);

            // Then
            assertNotSame(result1, result2);
            assertEquals(mockAdmin1, result1);
            assertEquals(mockAdmin2, result2);
            mockStaticAdmin.verify(() -> Admin.create(any(Properties.class)), times(2));
        }
    }

    @Test
    void shouldCreateConsumer() {
        try (var mockConstructionKafkaConsumer = mockConstruction(KafkaConsumer.class)) {
            // Given
            var clusterId = "test-cluster";

            // When
            var result = kafkaManager.getConsumer(clusterId);

            // Then
            assertSame(mockConstructionKafkaConsumer.constructed().getFirst(), result);
        }
    }

    @Test
    void shouldCreateProducer() {
        try (var mockConstructionKafkaProducer = mockConstruction(KafkaProducer.class)) {
            // Given
            var clusterId = "test-cluster";

            // When
            var result = kafkaManager.getProducer(clusterId);

            // Then
            assertSame(mockConstructionKafkaProducer.constructed().getFirst(), result);
        }
    }

    @Test
    void shouldCreateSchemaRegistryClient() {
        try (var mockStaticSchemaRegistryClientFactory = mockStatic(SchemaRegistryClientFactory.class)) {
            // Given
            var clusterId = "test-cluster";
            var mockSchemaRegistryClient = mock(SchemaRegistryClient.class);
            mockStaticSchemaRegistryClientFactory.when(() -> SchemaRegistryClientFactory.newClient(anyList(), anyInt(), anyList(), anyMap(), any()))
                    .thenReturn(mockSchemaRegistryClient);

            // When
            var result = kafkaManager.getSchemaRegistryClient(clusterId);

            // Then
            assertEquals(mockSchemaRegistryClient, result);
            mockStaticSchemaRegistryClientFactory.verify(() -> SchemaRegistryClientFactory.newClient(anyList(), anyInt(), anyList(), anyMap(), any()));
        }
    }

    @Test
    void shouldReturnSameSchemaRegistryClientForSameClusterId() {
        try (var mockStaticSchemaRegistryClientFactory = mockStatic(SchemaRegistryClientFactory.class)) {
            // Given
            var clusterId = "test-cluster";
            var mockSchemaRegistryClient = mock(SchemaRegistryClient.class);
            mockStaticSchemaRegistryClientFactory.when(() -> SchemaRegistryClientFactory.newClient(anyList(), anyInt(), anyList(), anyMap(), any()))
                    .thenReturn(mockSchemaRegistryClient);

            // When
            var result1 = kafkaManager.getSchemaRegistryClient(clusterId);
            var result2 = kafkaManager.getSchemaRegistryClient(clusterId);

            // Then
            assertSame(result1, result2);
            mockStaticSchemaRegistryClientFactory.verify(() -> SchemaRegistryClientFactory.newClient(anyList(), anyInt(), anyList(), anyMap(), any()));
        }
    }

    @Test
    void shouldCreateDifferentSchemaRegistryClientForDifferentClusterId() {
        try (var mockStaticSchemaRegistryClientFactory = mockStatic(SchemaRegistryClientFactory.class)) {
            // Given
            var clusterId1 = "test-cluster-1";
            var clusterId2 = "test-cluster-2";
            var mockSchemaRegistryClient1 = mock(SchemaRegistryClient.class);
            var mockSchemaRegistryClient2 = mock(SchemaRegistryClient.class);
            mockStaticSchemaRegistryClientFactory.when(() -> SchemaRegistryClientFactory.newClient(anyList(), anyInt(), anyList(), anyMap(), any()))
                    .thenReturn(mockSchemaRegistryClient1).thenReturn(mockSchemaRegistryClient2);

            // When
            var result1 = kafkaManager.getSchemaRegistryClient(clusterId1);
            var result2 = kafkaManager.getSchemaRegistryClient(clusterId2);

            // Then
            assertNotSame(result1, result2);
            assertEquals(mockSchemaRegistryClient1, result1);
            assertEquals(mockSchemaRegistryClient2, result2);
            mockStaticSchemaRegistryClientFactory.verify(() -> SchemaRegistryClientFactory.newClient(anyList(), anyInt(), anyList(), anyMap(), any()), times(2));
        }
    }

    @Test
    void shouldCreateConnectClient() {
        try (var mockConstructionKafkaConnectClient = mockConstruction(KafkaConnectClient.class)) {
            // Given
            var clusterId = "test-cluster";
            var connectId = "test-connect";

            // When
            var result = kafkaManager.getConnectClient(clusterId, connectId);

            // Then
            assertEquals(1, mockConstructionKafkaConnectClient.constructed().size());
            assertSame(mockConstructionKafkaConnectClient.constructed().get(0), result);
        }
    }

    @Test
    void shouldReturnSameConnectClientForSameClusterIdAndConnectId() {
        try (var mockConstructionKafkaConnectClient = mockConstruction(KafkaConnectClient.class)) {
            // Given
            var clusterId = "test-cluster";
            var connectId = "test-connect";

            // When
            var result1 = kafkaManager.getConnectClient(clusterId, connectId);
            var result2 = kafkaManager.getConnectClient(clusterId, connectId);

            // Then
            assertSame(result1, result2);
            assertEquals(1, mockConstructionKafkaConnectClient.constructed().size());
        }
    }

    @Test
    void shouldCreateDifferentConnectClientForDifferentConnectId() {
        try (var mockConstructionKafkaConnectClient = mockConstruction(KafkaConnectClient.class)) {
            // Given
            var clusterId = "test-cluster";
            var connectId1 = "test-connect-1";
            var connectId2 = "test-connect-2";

            // When
            var result1 = kafkaManager.getConnectClient(clusterId, connectId1);
            var result2 = kafkaManager.getConnectClient(clusterId, connectId2);

            // Then
            assertNotSame(result1, result2);
            assertEquals(2, mockConstructionKafkaConnectClient.constructed().size());
        }
    }

    @Test
    void shouldCreateDifferentConnectClientForDifferentClusterId() {
        try (var mockConstructionKafkaConnectClient = mockConstruction(KafkaConnectClient.class)) {
            // Given
            var clusterId1 = "test-cluster-1";
            var clusterId2 = "test-cluster-2";
            var connectId = "test-connect";

            // When
            var result1 = kafkaManager.getConnectClient(clusterId1, connectId);
            var result2 = kafkaManager.getConnectClient(clusterId2, connectId);

            // Then
            assertNotSame(result1, result2);
            assertEquals(2, mockConstructionKafkaConnectClient.constructed().size());
        }
    }

    class KafkaManager extends AbstractKafkaManager {

        @Override
        protected Properties getAdminClientProperties(String clusterId) {
            return new Properties();
        }

        @Override
        protected Properties getConsumerProperties(String clusterId) {
            return new Properties();
        }

        @Override
        protected Properties getProducerProperties(String clusterId) {
            return new Properties();
        }

        @Override
        protected Properties getSchemaRegistryClientProperties(String clusterId) {
            return new Properties();
        }

        @Override
        protected Properties getConnectClientProperties(String clusterId, String connectId) {
            return new Properties() {{
                put("url", "http://localhost:8083");
            }};
        }

        @Override
        protected List<String> getConnectIds(String clusterId) {
            return List.of();
        }

        @Override
        public boolean schemaRegistryIsDefined(String clusterId) {
            return true;
        }

        @Override
        public boolean connectsIsDefined(String clusterId) {
            return true;
        }
    }
}