package com.prafka.core.connection;

import lombok.Builder;

import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;

/**
 * Configuration properties for HTTP Basic authentication.
 *
 * <p>This class holds username and password credentials for Basic authentication,
 * used when connecting to HTTP-based services like Schema Registry or Kafka Connect.
 *
 * <p>Use the Lombok-generated builder to create instances:
 * <pre>{@code
 * BasicAuthenticationProperties props = BasicAuthenticationProperties.builder()
 *     .username("user")
 *     .password("secret".toCharArray())
 *     .build();
 * }</pre>
 *
 * @see ConnectProperties
 * @see SchemaRegistryProperties
 * @see AuthenticationMethod#BASIC
 */
@Builder
public class BasicAuthenticationProperties {

    private String username;
    private char[] password;

    public Properties properties(BiFunction<String, char[], Map<String, String>> function) {
        var properties = new Properties();
        properties.putAll(function.apply(username, password));
        return properties;
    }
}
