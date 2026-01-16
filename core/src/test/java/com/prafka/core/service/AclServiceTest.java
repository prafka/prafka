package com.prafka.core.service;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateAclsResult;
import org.apache.kafka.clients.admin.DescribeAclsResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.acl.*;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AclServiceTest {

    private AdminClient adminClient = mock(AdminClient.class);
    private AclService aclService = new AclService() {
        @Override
        protected AdminClient adminClient(String clusterId) {
            return adminClient;
        }
    };

    @Test
    void shouldGetAll() throws Exception {
        // Given
        var describeAclsResult = mock(DescribeAclsResult.class);
        var aclBinding1 = mock(AclBinding.class, RETURNS_DEEP_STUBS);
        var aclBinding2 = mock(AclBinding.class, RETURNS_DEEP_STUBS);
        var aclBindings = List.of(aclBinding1, aclBinding2);

        when(adminClient.describeAcls(any(AclBindingFilter.class))).thenReturn(describeAclsResult);
        when(describeAclsResult.values()).thenReturn(KafkaFuture.completedFuture(aclBindings));

        // When
        var result = aclService.getAll("clusterId").get();

        // Then
        assertEquals(2, result.size());
    }

    @Test
    void shouldCreate() throws Exception {
        // Given
        var aclBinding = new AclBinding(
                new ResourcePattern(ResourceType.TOPIC, "test-topic", PatternType.LITERAL),
                new AccessControlEntry("User:test-user", "*", AclOperation.READ, AclPermissionType.ALLOW)
        );
        var createAclsResult = mock(CreateAclsResult.class);

        when(adminClient.createAcls(Collections.singletonList(aclBinding))).thenReturn(createAclsResult);
        when(createAclsResult.all()).thenReturn(KafkaFuture.completedFuture(null));

        // When
        var result = aclService.create(
                "clusterId",
                "test-user",
                "*",
                ResourceType.TOPIC,
                AclPermissionType.ALLOW,
                AclOperation.READ,
                PatternType.LITERAL,
                "test-topic"
        ).get();

        // Then
        assertNotNull(result);
    }

    @Test
    void shouldCreateForConsumerWithExactTopicsAndGroups() throws Exception {
        // Given
        var exactTopics = List.of("topic1", "topic2");
        var exactConsumerGroups = List.of("group1", "group2");
        var aclBindings = new ArrayList<AclBinding>();

        var aclPrincipal = "User:test-user";
        var aclHost = "*";

        // Expected ACLs for topics
        exactTopics.forEach(topic -> {
            aclBindings.add(new AclBinding(
                    new ResourcePattern(ResourceType.TOPIC, topic, PatternType.LITERAL),
                    new AccessControlEntry(aclPrincipal, aclHost, AclOperation.READ, AclPermissionType.ALLOW)
            ));
            aclBindings.add(new AclBinding(
                    new ResourcePattern(ResourceType.TOPIC, topic, PatternType.LITERAL),
                    new AccessControlEntry(aclPrincipal, aclHost, AclOperation.DESCRIBE, AclPermissionType.ALLOW)
            ));
        });

        // Expected ACLs for consumer groups
        exactConsumerGroups.forEach(group -> {
            aclBindings.add(new AclBinding(
                    new ResourcePattern(ResourceType.GROUP, group, PatternType.LITERAL),
                    new AccessControlEntry(aclPrincipal, aclHost, AclOperation.READ, AclPermissionType.ALLOW)
            ));
        });

        var createAclsResult = mock(CreateAclsResult.class);

        when(adminClient.createAcls(aclBindings)).thenReturn(createAclsResult);
        when(createAclsResult.all()).thenReturn(KafkaFuture.completedFuture(null));

        // When
        var result = aclService.createForConsumer(
                "clusterId",
                "test-user",
                "*",
                exactTopics,
                null,
                exactConsumerGroups,
                null
        ).get();

        // Then
        assertEquals(6, result.size());
    }

    @Test
    void shouldCreateForConsumerWithPrefixedTopicsAndGroups() throws Exception {
        // Given
        var prefixedTopic = "test-prefix";
        var prefixedConsumerGroup = "group-prefix";
        var aclBindings = new ArrayList<AclBinding>();

        var aclPrincipal = "User:test-user";
        var aclHost = "*";

        // Expected ACLs for prefixed topics
        aclBindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TOPIC, prefixedTopic, PatternType.PREFIXED),
                new AccessControlEntry(aclPrincipal, aclHost, AclOperation.READ, AclPermissionType.ALLOW)
        ));
        aclBindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TOPIC, prefixedTopic, PatternType.PREFIXED),
                new AccessControlEntry(aclPrincipal, aclHost, AclOperation.DESCRIBE, AclPermissionType.ALLOW)
        ));

        // Expected ACLs for prefixed consumer groups
        aclBindings.add(new AclBinding(
                new ResourcePattern(ResourceType.GROUP, prefixedConsumerGroup, PatternType.PREFIXED),
                new AccessControlEntry(aclPrincipal, aclHost, AclOperation.READ, AclPermissionType.ALLOW)
        ));

        var createAclsResult = mock(CreateAclsResult.class);

        when(adminClient.createAcls(aclBindings)).thenReturn(createAclsResult);
        when(createAclsResult.all()).thenReturn(KafkaFuture.completedFuture(null));

        // When
        var result = aclService.createForConsumer(
                "clusterId",
                "test-user",
                "*",
                null,
                prefixedTopic,
                null,
                prefixedConsumerGroup
        ).get();

        // Then
        assertEquals(3, result.size());
    }

    @Test
    void shouldCreateForConsumerWithWildcardTopicsAndGroups() throws Exception {
        // Given
        var aclBindings = new ArrayList<AclBinding>();

        var aclPrincipal = "User:test-user";
        var aclHost = "*";

        // Expected ACLs for wildcard topics
        aclBindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TOPIC, ResourcePattern.WILDCARD_RESOURCE, PatternType.LITERAL),
                new AccessControlEntry(aclPrincipal, aclHost, AclOperation.READ, AclPermissionType.ALLOW)
        ));
        aclBindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TOPIC, ResourcePattern.WILDCARD_RESOURCE, PatternType.LITERAL),
                new AccessControlEntry(aclPrincipal, aclHost, AclOperation.DESCRIBE, AclPermissionType.ALLOW)
        ));

        // Expected ACLs for wildcard consumer groups
        aclBindings.add(new AclBinding(
                new ResourcePattern(ResourceType.GROUP, ResourcePattern.WILDCARD_RESOURCE, PatternType.LITERAL),
                new AccessControlEntry(aclPrincipal, aclHost, AclOperation.READ, AclPermissionType.ALLOW)
        ));

        var createAclsResult = mock(CreateAclsResult.class);

        when(adminClient.createAcls(aclBindings)).thenReturn(createAclsResult);
        when(createAclsResult.all()).thenReturn(KafkaFuture.completedFuture(null));

        // When
        var result = aclService.createForConsumer(
                "clusterId",
                "test-user",
                "*",
                null,
                null,
                null,
                null
        ).get();

        // Then
        assertEquals(3, result.size());
    }

    @Test
    void shouldCreateForProducerWithExactTopics() throws Exception {
        // Given
        var exactTopics = List.of("topic1", "topic2");
        var aclBindings = new ArrayList<AclBinding>();

        var aclPrincipal = "User:test-user";
        var aclHost = "*";

        // Expected ACLs for topics
        exactTopics.forEach(topic -> {
            aclBindings.add(new AclBinding(
                    new ResourcePattern(ResourceType.TOPIC, topic, PatternType.LITERAL),
                    new AccessControlEntry(aclPrincipal, aclHost, AclOperation.CREATE, AclPermissionType.ALLOW)
            ));
            aclBindings.add(new AclBinding(
                    new ResourcePattern(ResourceType.TOPIC, topic, PatternType.LITERAL),
                    new AccessControlEntry(aclPrincipal, aclHost, AclOperation.WRITE, AclPermissionType.ALLOW)
            ));
            aclBindings.add(new AclBinding(
                    new ResourcePattern(ResourceType.TOPIC, topic, PatternType.LITERAL),
                    new AccessControlEntry(aclPrincipal, aclHost, AclOperation.DESCRIBE, AclPermissionType.ALLOW)
            ));
        });

        // Expected ACLs for wildcard transactional IDs
        aclBindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TRANSACTIONAL_ID, ResourcePattern.WILDCARD_RESOURCE, PatternType.LITERAL),
                new AccessControlEntry(aclPrincipal, aclHost, AclOperation.WRITE, AclPermissionType.ALLOW)
        ));
        aclBindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TRANSACTIONAL_ID, ResourcePattern.WILDCARD_RESOURCE, PatternType.LITERAL),
                new AccessControlEntry(aclPrincipal, aclHost, AclOperation.DESCRIBE, AclPermissionType.ALLOW)
        ));

        var createAclsResult = mock(CreateAclsResult.class);

        when(adminClient.createAcls(aclBindings)).thenReturn(createAclsResult);
        when(createAclsResult.all()).thenReturn(KafkaFuture.completedFuture(null));

        // When
        var result = aclService.createForProducer(
                "clusterId",
                "test-user",
                "*",
                exactTopics,
                null,
                null,
                null,
                false
        ).get();

        // Then
        assertEquals(8, result.size());
    }

    @Test
    void shouldCreateForProducerWithPrefixedTopics() throws Exception {
        // Given
        var prefixedTopic = "test-prefix";
        var aclBindings = new ArrayList<AclBinding>();

        var aclPrincipal = "User:test-user";
        var aclHost = "*";

        // Expected ACLs for prefixed topics
        aclBindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TOPIC, prefixedTopic, PatternType.PREFIXED),
                new AccessControlEntry(aclPrincipal, aclHost, AclOperation.CREATE, AclPermissionType.ALLOW)
        ));
        aclBindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TOPIC, prefixedTopic, PatternType.PREFIXED),
                new AccessControlEntry(aclPrincipal, aclHost, AclOperation.WRITE, AclPermissionType.ALLOW)
        ));
        aclBindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TOPIC, prefixedTopic, PatternType.PREFIXED),
                new AccessControlEntry(aclPrincipal, aclHost, AclOperation.DESCRIBE, AclPermissionType.ALLOW)
        ));

        // Expected ACLs for wildcard transactional IDs
        aclBindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TRANSACTIONAL_ID, ResourcePattern.WILDCARD_RESOURCE, PatternType.LITERAL),
                new AccessControlEntry(aclPrincipal, aclHost, AclOperation.WRITE, AclPermissionType.ALLOW)
        ));
        aclBindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TRANSACTIONAL_ID, ResourcePattern.WILDCARD_RESOURCE, PatternType.LITERAL),
                new AccessControlEntry(aclPrincipal, aclHost, AclOperation.DESCRIBE, AclPermissionType.ALLOW)
        ));

        var createAclsResult = mock(CreateAclsResult.class);

        when(adminClient.createAcls(aclBindings)).thenReturn(createAclsResult);
        when(createAclsResult.all()).thenReturn(KafkaFuture.completedFuture(null));

        // When
        var result = aclService.createForProducer(
                "clusterId",
                "test-user",
                "*",
                null,
                prefixedTopic,
                null,
                null,
                false
        ).get();

        // Then
        assertEquals(5, result.size());
    }

    @Test
    void shouldCreateForProducerWithIdempotentFlag() throws Exception {
        // Given
        var exactTopics = List.of("topic1");
        var aclBindings = new ArrayList<AclBinding>();

        var aclPrincipal = "User:test-user";
        var aclHost = "*";

        // Expected ACLs with IDEMPOTENT_WRITE instead of WRITE
        exactTopics.forEach(topic -> {
            aclBindings.add(new AclBinding(
                    new ResourcePattern(ResourceType.TOPIC, topic, PatternType.LITERAL),
                    new AccessControlEntry(aclPrincipal, aclHost, AclOperation.CREATE, AclPermissionType.ALLOW)
            ));
            aclBindings.add(new AclBinding(
                    new ResourcePattern(ResourceType.TOPIC, topic, PatternType.LITERAL),
                    new AccessControlEntry(aclPrincipal, aclHost, AclOperation.IDEMPOTENT_WRITE, AclPermissionType.ALLOW)
            ));
            aclBindings.add(new AclBinding(
                    new ResourcePattern(ResourceType.TOPIC, topic, PatternType.LITERAL),
                    new AccessControlEntry(aclPrincipal, aclHost, AclOperation.DESCRIBE, AclPermissionType.ALLOW)
            ));
        });

        // Expected ACLs for wildcard transactional IDs
        aclBindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TRANSACTIONAL_ID, ResourcePattern.WILDCARD_RESOURCE, PatternType.LITERAL),
                new AccessControlEntry(aclPrincipal, aclHost, AclOperation.WRITE, AclPermissionType.ALLOW)
        ));
        aclBindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TRANSACTIONAL_ID, ResourcePattern.WILDCARD_RESOURCE, PatternType.LITERAL),
                new AccessControlEntry(aclPrincipal, aclHost, AclOperation.DESCRIBE, AclPermissionType.ALLOW)
        ));

        var createAclsResult = mock(CreateAclsResult.class);

        when(adminClient.createAcls(aclBindings)).thenReturn(createAclsResult);
        when(createAclsResult.all()).thenReturn(KafkaFuture.completedFuture(null));

        // When
        var result = aclService.createForProducer(
                "clusterId",
                "test-user",
                "*",
                exactTopics,
                null,
                null,
                null,
                true
        ).get();

        // Then
        assertEquals(5, result.size());
    }

    @Test
    void shouldCreateForProducerWithTransactionalIds() throws Exception {
        // Given
        var exactTransactionalId = "txn-123";
        var prefixedTransactionalId = "txn-prefix";
        var aclBindings = new ArrayList<AclBinding>();

        var aclPrincipal = "User:test-user";
        var aclHost = "*";

        // Expected ACLs for wildcard topics
        aclBindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TOPIC, ResourcePattern.WILDCARD_RESOURCE, PatternType.LITERAL),
                new AccessControlEntry(aclPrincipal, aclHost, AclOperation.CREATE, AclPermissionType.ALLOW)
        ));
        aclBindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TOPIC, ResourcePattern.WILDCARD_RESOURCE, PatternType.LITERAL),
                new AccessControlEntry(aclPrincipal, aclHost, AclOperation.WRITE, AclPermissionType.ALLOW)
        ));
        aclBindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TOPIC, ResourcePattern.WILDCARD_RESOURCE, PatternType.LITERAL),
                new AccessControlEntry(aclPrincipal, aclHost, AclOperation.DESCRIBE, AclPermissionType.ALLOW)
        ));

        // Expected ACLs for transactional IDs
        aclBindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TRANSACTIONAL_ID, exactTransactionalId, PatternType.LITERAL),
                new AccessControlEntry(aclPrincipal, aclHost, AclOperation.WRITE, AclPermissionType.ALLOW)
        ));
        aclBindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TRANSACTIONAL_ID, exactTransactionalId, PatternType.LITERAL),
                new AccessControlEntry(aclPrincipal, aclHost, AclOperation.DESCRIBE, AclPermissionType.ALLOW)
        ));

        aclBindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TRANSACTIONAL_ID, prefixedTransactionalId, PatternType.PREFIXED),
                new AccessControlEntry(aclPrincipal, aclHost, AclOperation.WRITE, AclPermissionType.ALLOW)
        ));
        aclBindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TRANSACTIONAL_ID, prefixedTransactionalId, PatternType.PREFIXED),
                new AccessControlEntry(aclPrincipal, aclHost, AclOperation.DESCRIBE, AclPermissionType.ALLOW)
        ));

        var createAclsResult = mock(CreateAclsResult.class);

        when(adminClient.createAcls(aclBindings)).thenReturn(createAclsResult);
        when(createAclsResult.all()).thenReturn(KafkaFuture.completedFuture(null));

        // When
        var result = aclService.createForProducer(
                "clusterId",
                "test-user",
                "*",
                null,
                null,
                exactTransactionalId,
                prefixedTransactionalId,
                false
        ).get();

        // Then
        assertEquals(7, result.size());
    }

    @Test
    void shouldGetAllAclSummary() throws Exception {
        // Given
        var describeAclsResult = mock(DescribeAclsResult.class);

        var aclBindings = List.of(
                new AclBinding(
                        new ResourcePattern(ResourceType.TOPIC, "test-topic", PatternType.LITERAL),
                        new AccessControlEntry("User:test-user", "*", AclOperation.READ, AclPermissionType.ALLOW)
                ),
                new AclBinding(
                        new ResourcePattern(ResourceType.TOPIC, "another-topic", PatternType.LITERAL),
                        new AccessControlEntry("User:test-user", "*", AclOperation.WRITE, AclPermissionType.ALLOW)
                ),
                new AclBinding(
                        new ResourcePattern(ResourceType.GROUP, "test-group", PatternType.PREFIXED),
                        new AccessControlEntry("User:admin", "192.168.1.1", AclOperation.WRITE, AclPermissionType.DENY)
                ),
                new AclBinding(
                        new ResourcePattern(ResourceType.GROUP, "another-group", PatternType.LITERAL),
                        new AccessControlEntry("User:admin", "*", AclOperation.READ, AclPermissionType.ALLOW)
                ),
                new AclBinding(
                        new ResourcePattern(ResourceType.CLUSTER, "kafka-cluster", PatternType.LITERAL),
                        new AccessControlEntry("User:super-user", "*", AclOperation.ALL, AclPermissionType.ALLOW)
                )
        );

        when(adminClient.describeAcls(any(AclBindingFilter.class))).thenReturn(describeAclsResult);
        when(describeAclsResult.values()).thenReturn(KafkaFuture.completedFuture(aclBindings));

        // When
        var result = aclService.getAllAclSummary("clusterId").get();

        // Then
        assertEquals(5, result.aclCount());
        assertEquals(3, result.principalCount());
        assertEquals(2, result.topicCount());
        assertEquals(2, result.groupCount());
    }
}