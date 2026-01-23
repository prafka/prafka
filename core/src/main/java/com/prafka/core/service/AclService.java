package com.prafka.core.service;

import com.prafka.core.model.Acl;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.acl.*;
import org.apache.kafka.common.resource.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.apache.kafka.common.resource.ResourcePattern.WILDCARD_RESOURCE;

/**
 * Service for managing Kafka Access Control Lists (ACLs).
 *
 * <p>Provides operations to list, create, and delete ACLs for various Kafka resources
 * including topics, consumer groups, cluster, transactional IDs, and delegation tokens.
 * Also includes helper methods to create ACLs for typical consumer and producer use cases.
 *
 * @see Acl
 */
@Named
@Singleton
public class AclService extends AbstractService {

    private static final Map<ResourceType, List<AclOperation>> RESOURCE_TO_OPERATIONS = new LinkedHashMap<>() {{
        put(ResourceType.TOPIC, List.of(
                AclOperation.ALL,
                AclOperation.ALTER,
                AclOperation.ALTER_CONFIGS,
                AclOperation.CREATE,
                AclOperation.DELETE,
                AclOperation.DESCRIBE,
                AclOperation.DESCRIBE_CONFIGS,
                AclOperation.READ,
                AclOperation.WRITE,
                AclOperation.IDEMPOTENT_WRITE
        ));
        put(ResourceType.GROUP, List.of(
                AclOperation.ALL,
                AclOperation.DELETE,
                AclOperation.DESCRIBE,
                AclOperation.READ
        ));
        put(ResourceType.CLUSTER, List.of(
                AclOperation.ALL,
                AclOperation.ALTER,
                AclOperation.ALTER_CONFIGS,
                AclOperation.CLUSTER_ACTION,
                AclOperation.CREATE,
                AclOperation.DESCRIBE,
                AclOperation.DESCRIBE_CONFIGS
        ));
        put(ResourceType.TRANSACTIONAL_ID, List.of(
                AclOperation.ALL,
                AclOperation.DESCRIBE,
                AclOperation.WRITE
        ));
        put(ResourceType.DELEGATION_TOKEN, List.of(
                AclOperation.ALL,
                AclOperation.DESCRIBE
        ));
    }};

    public Map<ResourceType, List<AclOperation>> getResourceToOperations() {
        return RESOURCE_TO_OPERATIONS;
    }

    public List<AclOperation> getOperationsByResource(ResourceType resource) {
        return RESOURCE_TO_OPERATIONS.get(resource);
    }

    public CompletableFuture<List<Acl>> getAll(String clusterId) {
        return getAll(clusterId, AclBindingFilter.ANY);
    }

    public CompletableFuture<List<Acl>> getAllByResource(String clusterId, ResourceType resource, String pattern) {
        return getAll(clusterId, new AclBindingFilter(new ResourcePatternFilter(resource, pattern, PatternType.MATCH), AccessControlEntryFilter.ANY));
    }

    private CompletableFuture<List<Acl>> getAll(String clusterId, AclBindingFilter filter) {
        return adminClient(clusterId)
                .describeAcls(filter)
                .values()
                .thenApply(it -> it.stream().map(Acl::new).toList())
                .toCompletionStage()
                .toCompletableFuture();
    }

    public CompletableFuture<Acl> create(String clusterId,
                                         String principal, String host,
                                         ResourceType resource, AclPermissionType permission, AclOperation operation,
                                         PatternType patternType, String patternValue) {
        var acl = new AclBinding(
                resource == ResourceType.CLUSTER
                        ? new ResourcePattern(resource, Resource.CLUSTER_NAME, PatternType.LITERAL)
                        : new ResourcePattern(resource, Optional.ofNullable(patternValue).orElse(WILDCARD_RESOURCE), patternType),
                new AccessControlEntry(getPrincipal(principal), getHost(host), operation, permission)
        );
        return adminClient(clusterId)
                .createAcls(Collections.singletonList(acl))
                .all()
                .thenApply(it -> new Acl(acl))
                .toCompletionStage()
                .toCompletableFuture();
    }

    public CompletableFuture<List<Acl>> createForConsumer(String clusterId,
                                                          String principal, String host,
                                                          List<String> exactTopics, String prefixedTopic,
                                                          List<String> exactConsumerGroups, String prefixedConsumerGroup) {
        var aclList = new ArrayList<AclBinding>();

        var aclPrincipal = getPrincipal(principal);
        var aclHost = getHost(host);

        if (CollectionUtils.isNotEmpty(exactTopics)) {
            exactTopics.forEach(topic -> {
                aclList.add(new AclBinding(topicLiteral(topic), allowRead(aclPrincipal, aclHost)));
                aclList.add(new AclBinding(topicLiteral(topic), allowDescribe(aclPrincipal, aclHost)));
            });
        }
        if (StringUtils.isNotBlank(prefixedTopic)) {
            aclList.add(new AclBinding(topicPrefixed(prefixedTopic), allowRead(aclPrincipal, aclHost)));
            aclList.add(new AclBinding(topicPrefixed(prefixedTopic), allowDescribe(aclPrincipal, aclHost)));
        }
        if (CollectionUtils.isEmpty(exactTopics) && StringUtils.isBlank(prefixedTopic)) {
            aclList.add(new AclBinding(topicLiteral(WILDCARD_RESOURCE), allowRead(aclPrincipal, aclHost)));
            aclList.add(new AclBinding(topicLiteral(WILDCARD_RESOURCE), allowDescribe(aclPrincipal, aclHost)));
        }

        if (CollectionUtils.isNotEmpty(exactConsumerGroups)) {
            exactConsumerGroups.forEach(consumerGroup -> aclList.add(new AclBinding(groupLiteral(consumerGroup), allowRead(aclPrincipal, aclHost))));
        }
        if (StringUtils.isNotBlank(prefixedConsumerGroup)) {
            aclList.add(new AclBinding(groupPrefixed(prefixedConsumerGroup), allowRead(aclPrincipal, aclHost)));
        }
        if (CollectionUtils.isEmpty(exactConsumerGroups) && StringUtils.isBlank(prefixedConsumerGroup)) {
            aclList.add(new AclBinding(groupLiteral(WILDCARD_RESOURCE), allowRead(aclPrincipal, aclHost)));
        }

        return adminClient(clusterId)
                .createAcls(aclList)
                .all()
                .thenApply(it -> aclList.stream().map(Acl::new).toList())
                .toCompletionStage()
                .toCompletableFuture();
    }

    public CompletableFuture<List<Acl>> createForProducer(String clusterId,
                                                          String principal, String host,
                                                          List<String> exactTopics, String prefixedTopic,
                                                          String exactTransactionalId, String prefixedTransactionalId,
                                                          Boolean idempotent) {
        var aclList = new ArrayList<AclBinding>();

        var aclPrincipal = getPrincipal(principal);
        var aclHost = getHost(host);

        if (CollectionUtils.isNotEmpty(exactTopics)) {
            exactTopics.forEach(topic -> {
                aclList.add(new AclBinding(topicLiteral(topic), allowCreate(aclPrincipal, aclHost)));
                aclList.add(new AclBinding(topicLiteral(topic), idempotent ? allowWriteIdempotent(aclPrincipal, aclHost) : allowWrite(aclPrincipal, aclHost)));
                aclList.add(new AclBinding(topicLiteral(topic), allowDescribe(aclPrincipal, aclHost)));
            });
        }

        if (StringUtils.isNotBlank(prefixedTopic)) {
            aclList.add(new AclBinding(topicPrefixed(prefixedTopic), allowCreate(aclPrincipal, aclHost)));
            aclList.add(new AclBinding(topicPrefixed(prefixedTopic), idempotent ? allowWriteIdempotent(aclPrincipal, aclHost) : allowWrite(aclPrincipal, aclHost)));
            aclList.add(new AclBinding(topicPrefixed(prefixedTopic), allowDescribe(aclPrincipal, aclHost)));
        }
        if (CollectionUtils.isEmpty(exactTopics) && StringUtils.isBlank(prefixedTopic)) {
            aclList.add(new AclBinding(topicLiteral(WILDCARD_RESOURCE), allowCreate(aclPrincipal, aclHost)));
            aclList.add(new AclBinding(topicLiteral(WILDCARD_RESOURCE), idempotent ? allowWriteIdempotent(aclPrincipal, aclHost) : allowWrite(aclPrincipal, aclHost)));
            aclList.add(new AclBinding(topicLiteral(WILDCARD_RESOURCE), allowDescribe(aclPrincipal, aclHost)));
        }

        if (StringUtils.isNotBlank(exactTransactionalId)) {
            aclList.add(new AclBinding(transactionalIdLiteral(exactTransactionalId), allowWrite(aclPrincipal, aclHost)));
            aclList.add(new AclBinding(transactionalIdLiteral(exactTransactionalId), allowDescribe(aclPrincipal, aclHost)));
        }
        if (StringUtils.isNotBlank(prefixedTransactionalId)) {
            aclList.add(new AclBinding(transactionalIdPrefixed(prefixedTransactionalId), allowWrite(aclPrincipal, aclHost)));
            aclList.add(new AclBinding(transactionalIdPrefixed(prefixedTransactionalId), allowDescribe(aclPrincipal, aclHost)));
        }
        if (StringUtils.isBlank(exactTransactionalId) && StringUtils.isBlank(prefixedTransactionalId)) {
            aclList.add(new AclBinding(transactionalIdLiteral(WILDCARD_RESOURCE), allowWrite(aclPrincipal, aclHost)));
            aclList.add(new AclBinding(transactionalIdLiteral(WILDCARD_RESOURCE), allowDescribe(aclPrincipal, aclHost)));
        }

        return adminClient(clusterId)
                .createAcls(aclList)
                .all()
                .thenApply(it -> aclList.stream().map(Acl::new).toList())
                .toCompletionStage()
                .toCompletableFuture();
    }

    public CompletableFuture<Void> delete(String clusterId, AclBindingFilter acl) {
        return deleteAll(clusterId, Collections.singletonList(acl));
    }

    public CompletableFuture<Void> deleteAll(String clusterId, List<AclBindingFilter> aclList) {
        return adminClient(clusterId)
                .deleteAcls(aclList)
                .all()
                .thenApply(it -> (Void) null)
                .toCompletionStage()
                .toCompletableFuture();
    }

    public record AllAclSummary(int aclCount, long principalCount, long topicCount, long groupCount) {
    }

    public CompletableFuture<AllAclSummary> getAllAclSummary(String clusterId) {
        return getAll(clusterId)
                .thenApply(list -> new AllAclSummary(
                        list.size(),
                        list.stream().map(Acl::getPrincipal).distinct().count(),
                        list.stream().filter(it -> it.getResource() == ResourceType.TOPIC).count(),
                        list.stream().filter(it -> it.getResource() == ResourceType.GROUP).count()
                ));
    }

    private static ResourcePattern topicLiteral(String name) {
        return new ResourcePattern(ResourceType.TOPIC, name, PatternType.LITERAL);
    }

    private static ResourcePattern topicPrefixed(String name) {
        return new ResourcePattern(ResourceType.TOPIC, name, PatternType.PREFIXED);
    }

    private static ResourcePattern groupLiteral(String name) {
        return new ResourcePattern(ResourceType.GROUP, name, PatternType.LITERAL);
    }

    private static ResourcePattern groupPrefixed(String name) {
        return new ResourcePattern(ResourceType.GROUP, name, PatternType.PREFIXED);
    }

    private static ResourcePattern transactionalIdLiteral(String name) {
        return new ResourcePattern(ResourceType.TRANSACTIONAL_ID, name, PatternType.LITERAL);
    }

    private static ResourcePattern transactionalIdPrefixed(String name) {
        return new ResourcePattern(ResourceType.TRANSACTIONAL_ID, name, PatternType.PREFIXED);
    }

    private static AccessControlEntry allowRead(String principal, String host) {
        return new AccessControlEntry(principal, host, AclOperation.READ, AclPermissionType.ALLOW);
    }

    private static AccessControlEntry allowWrite(String principal, String host) {
        return new AccessControlEntry(principal, host, AclOperation.WRITE, AclPermissionType.ALLOW);
    }

    private static AccessControlEntry allowWriteIdempotent(String principal, String host) {
        return new AccessControlEntry(principal, host, AclOperation.IDEMPOTENT_WRITE, AclPermissionType.ALLOW);
    }

    private static AccessControlEntry allowCreate(String principal, String host) {
        return new AccessControlEntry(principal, host, AclOperation.CREATE, AclPermissionType.ALLOW);
    }

    private static AccessControlEntry allowDescribe(String principal, String host) {
        return new AccessControlEntry(principal, host, AclOperation.DESCRIBE, AclPermissionType.ALLOW);
    }

    private static String getPrincipal(String principal) {
        if (StringUtils.isBlank(principal)) {
            return "User:" + WILDCARD_RESOURCE;
        } else {
            return "User:" + principal;
        }
    }

    private static String getHost(String host) {
        if (StringUtils.isBlank(host)) {
            return WILDCARD_RESOURCE;
        } else {
            return host;
        }
    }
}
