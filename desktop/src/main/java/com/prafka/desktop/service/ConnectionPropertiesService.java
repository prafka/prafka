package com.prafka.desktop.service;

import com.prafka.core.connection.*;
import com.prafka.desktop.model.ClusterModel;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;

import java.util.Properties;

@Singleton
public class ConnectionPropertiesService {

    public Properties getKafkaProperties(ClusterModel cluster) {
        var kafkaProperties = KafkaProperties.builder()
                .bootstrapServers(cluster.getBootstrapServers())
                .authenticationMethod(cluster.getAuthenticationMethod())
                .additionalProperties(cluster.getAdditionalProperties());
        if (cluster.getAuthenticationMethod() == AuthenticationMethod.SASL) {
            var saslAuthenticationProperties = SaslAuthenticationProperties.builder()
                    .securityProtocol(cluster.getSaslAuthentication().getSecurityProtocol())
                    .mechanism(cluster.getSaslAuthentication().getSaslMechanism())
                    .username(cluster.getSaslAuthentication().getUsername())
                    .password(cluster.getSaslAuthentication().getPassword());
            kafkaProperties.saslAuthenticationProperties(saslAuthenticationProperties);
        }
        if (cluster.getAuthenticationMethod() == AuthenticationMethod.SSL) {
            var sslAuthenticationProperties = getSslAuthenticationProperties(cluster.getSslAuthentication());
            kafkaProperties.sslAuthenticationProperties(sslAuthenticationProperties);
        }
        return kafkaProperties.build().properties();
    }

    public Properties getSchemaRegistryProperties(ClusterModel.SchemaRegistryModel schemaRegistry) {
        var schemaRegistryProperties = SchemaRegistryProperties.builder()
                .url(schemaRegistry.getUrl())
                .authenticationMethod(schemaRegistry.getAuthenticationMethod())
                .additionalProperties(schemaRegistry.getAdditionalProperties());
        if (schemaRegistry.getAuthenticationMethod() == AuthenticationMethod.BASIC) {
            var basicAuthenticationProperties = BasicAuthenticationProperties.builder()
                    .username(schemaRegistry.getBasicAuthentication().getUsername())
                    .password(schemaRegistry.getBasicAuthentication().getPassword());
            schemaRegistryProperties.basicAuthenticationProperties(basicAuthenticationProperties);
        }
        if (schemaRegistry.getAuthenticationMethod() == AuthenticationMethod.TOKEN) {
            var tokenAuthenticationProperties = TokenAuthenticationProperties.builder()
                    .token(StringUtils.valueOf(schemaRegistry.getTokenAuthentication().getToken()));
            schemaRegistryProperties.tokenAuthenticationProperties(tokenAuthenticationProperties);
        }
        if (schemaRegistry.getAuthenticationMethod() == AuthenticationMethod.SSL) {
            var sslAuthenticationProperties = getSslAuthenticationProperties(schemaRegistry.getSslAuthentication());
            schemaRegistryProperties.sslAuthenticationProperties(sslAuthenticationProperties);
        }
        return schemaRegistryProperties.build().properties();
    }

    public Properties getConnectProperties(ClusterModel.ConnectModel connect) {
        var connectProperties = ConnectProperties.builder()
                .url(connect.getUrl())
                .authenticationMethod(connect.getAuthenticationMethod())
                .additionalProperties(connect.getAdditionalProperties());
        if (connect.getAuthenticationMethod() == AuthenticationMethod.BASIC) {
            var basicAuthenticationProperties = BasicAuthenticationProperties.builder()
                    .username(connect.getBasicAuthentication().getUsername())
                    .password(connect.getBasicAuthentication().getPassword());
            connectProperties.basicAuthenticationProperties(basicAuthenticationProperties);
        }
        if (connect.getAuthenticationMethod() == AuthenticationMethod.SSL) {
            var sslAuthenticationProperties = getSslAuthenticationProperties(connect.getSslAuthentication());
            connectProperties.sslAuthenticationProperties(sslAuthenticationProperties);
        }
        return connectProperties.build().properties();
    }

    private SslAuthenticationProperties.Builder getSslAuthenticationProperties(ClusterModel.SslAuthentication sslAuthentication) {
        return SslAuthenticationProperties.builder()
                .keystoreLocation(sslAuthentication.getKeystoreLocation())
                .keystorePassword(StringUtils.valueOf(sslAuthentication.getKeystorePassword()))
                .keyPassword(StringUtils.valueOf(sslAuthentication.getKeyPassword()))
                .truststoreLocation(sslAuthentication.getTruststoreLocation())
                .truststorePassword(StringUtils.valueOf(sslAuthentication.getTruststorePassword()));
    }
}
