package com.prafka.core.model;

import lombok.Getter;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourceType;

@Getter
public class Acl {

    private final String principal;
    private final String host;
    private final ResourceType resource;
    private final AclPermissionType permission;
    private final AclOperation operation;
    private final PatternType patternType;
    private final String patternValue;

    public Acl(AclBinding source) {
        principal = source.entry().principal();
        host = source.entry().host();
        resource = source.pattern().resourceType();
        permission = source.entry().permissionType();
        operation = source.entry().operation();
        patternType = source.pattern().patternType();
        patternValue = source.pattern().name();
    }
}
