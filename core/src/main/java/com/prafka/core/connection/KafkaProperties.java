package com.prafka.core.connection;

import lombok.Builder;
import org.apache.kafka.clients.admin.AdminClientConfig;

import java.util.Map;
import java.util.Properties;

/**
 * Configuration properties for connecting to a Kafka cluster.
 *
 * <p>This class encapsulates all the connection settings needed to establish a connection
 * to Kafka brokers, including bootstrap servers, authentication method, and optional
 * SASL or SSL authentication properties.
 *
 * <p>Use the Lombok-generated builder to create instances:
 * <pre>{@code
 * KafkaProperties props = KafkaProperties.builder()
 *     .bootstrapServers("localhost:9092")
 *     .authenticationMethod(AuthenticationMethod.NONE)
 *     .build();
 * }</pre>
 *
 * @see AuthenticationMethod
 * @see SaslAuthenticationProperties
 * @see SslAuthenticationProperties
 */
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
