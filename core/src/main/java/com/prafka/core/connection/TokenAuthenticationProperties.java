package com.prafka.core.connection;

import lombok.Builder;

import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

/**
 * Configuration properties for token-based authentication.
 *
 * <p>This class holds a bearer token for authentication, typically used with OAuth2
 * or similar token-based authentication schemes when connecting to services like Schema Registry.
 *
 * <p>Use the Lombok-generated builder to create instances:
 * <pre>{@code
 * TokenAuthenticationProperties props = TokenAuthenticationProperties.builder()
 *     .token("your-bearer-token".toCharArray())
 *     .build();
 * }</pre>
 *
 * @see SchemaRegistryProperties
 * @see AuthenticationMethod#TOKEN
 */
@Builder
public class TokenAuthenticationProperties {

    private char[] token;

    public Properties properties(Function<char[], Map<String, String>> function) {
        var properties = new Properties();
        properties.putAll(function.apply(token));
        return properties;
    }
}
