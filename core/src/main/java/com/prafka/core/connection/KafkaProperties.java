package com.prafka.core.connection;

import lombok.Builder;
import org.apache.kafka.clients.admin.AdminClientConfig;

import java.util.Map;
import java.util.Properties;

@Builder
public class KafkaProperties {

    private String bootstrapServers;
    private AuthenticationMethod authenticationMethod;
    private SaslAuthenticationProperties.Builder saslAuthenticationProperties;
    private SslAuthenticationProperties.Builder sslAuthenticationProperties;
    private Map<String, String> additionalProperties;

    public Properties properties() {
        var properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        if (authenticationMethod == AuthenticationMethod.SASL) {
            properties.putAll(saslAuthenticationProperties.build().properties());
        }
        if (authenticationMethod == AuthenticationMethod.SSL) {
            properties.putAll(sslAuthenticationProperties.build().properties());
        }
        if (additionalProperties != null) {
            properties.putAll(additionalProperties);
        }
        return properties;
    }
}
