package com.prafka.desktop.model;

import com.prafka.core.connection.AuthenticationMethod;
import com.prafka.core.connection.SaslMechanism;
import com.prafka.core.connection.SaslSecurityProtocol;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Getter
@Setter
public class ClusterModel extends AbstractTrackedModel {

    private String name;
    private boolean current;
    private String bootstrapServers;
    private AuthenticationMethod authenticationMethod;
    private SaslAuthentication saslAuthentication;
    private SslAuthentication sslAuthentication;
    private LinkedHashMap<String, String> additionalProperties;
    private SchemaRegistryModel schemaRegistry;
    private List<ConnectModel> connects;

    public boolean isSchemaRegistryDefined() {
        return schemaRegistry != null && isNotBlank(schemaRegistry.getUrl());
    }

    public boolean isConnectsDefined() {
        return CollectionUtils.isNotEmpty(connects);
    }

    @Getter
    @Setter
    public static class BasicAuthentication {

        private String username;
        private char[] password;
    }

    @Getter
    @Setter
    public static class TokenAuthentication {

        private char[] token;
    }

    @Getter
    @Setter
    public static class SaslAuthentication {

        private SaslSecurityProtocol securityProtocol;
        private SaslMechanism saslMechanism;
        private String username;
        private char[] password;
    }

    @Getter
    @Setter
    public static class SslAuthentication {

        private String keystoreLocation;
        private char[] keystorePassword;
        private char[] keyPassword;
        private String truststoreLocation;
        private char[] truststorePassword;
    }

    @Getter
    @Setter
    public static class SchemaRegistryModel extends AbstractTrackedModel {

        private String url;
        private AuthenticationMethod authenticationMethod;
        private BasicAuthentication basicAuthentication;
        private TokenAuthentication tokenAuthentication;
        private SslAuthentication sslAuthentication;
        private LinkedHashMap<String, String> additionalProperties;
        private transient volatile Properties properties;
    }

    @Getter
    @Setter
    public static class ConnectModel extends AbstractTrackedModel {

        private String name;
        private String url;
        private AuthenticationMethod authenticationMethod;
        private BasicAuthentication basicAuthentication;
        private SslAuthentication sslAuthentication;
        private LinkedHashMap<String, String> additionalProperties;
        private transient volatile Properties properties;
    }
}
