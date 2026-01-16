package com.prafka.core.connection;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Builder
public class SchemaRegistryProperties {

    private String url;
    private AuthenticationMethod authenticationMethod;
    private BasicAuthenticationProperties.Builder basicAuthenticationProperties;
    private TokenAuthenticationProperties.Builder tokenAuthenticationProperties;
    private SslAuthenticationProperties.Builder sslAuthenticationProperties;
    private Map<String, String> additionalProperties;

    public Properties properties() {
        var properties = new Properties();
        properties.put(SchemaRegistryClientConfig.CLIENT_NAMESPACE + "url", url);
        if (authenticationMethod == AuthenticationMethod.BASIC) {
            properties.put(SchemaRegistryClientConfig.CLIENT_NAMESPACE + SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
            properties.putAll(
                    basicAuthenticationProperties.build().properties((username, password) -> {
                        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
                            return Map.of(SchemaRegistryClientConfig.CLIENT_NAMESPACE + SchemaRegistryClientConfig.USER_INFO_CONFIG, username + ":" + password);
                        }
                        return Collections.emptyMap();
                    })
            );
        }
        if (authenticationMethod == AuthenticationMethod.TOKEN) {
            properties.put(SchemaRegistryClientConfig.CLIENT_NAMESPACE + SchemaRegistryClientConfig.BEARER_AUTH_CREDENTIALS_SOURCE, "STATIC_TOKEN");
            properties.putAll(
                    tokenAuthenticationProperties.build().properties(token -> {
                        if (StringUtils.isNotBlank(token)) {
                            return Map.of(SchemaRegistryClientConfig.CLIENT_NAMESPACE + SchemaRegistryClientConfig.BEARER_AUTH_TOKEN_CONFIG, token);
                        }
                        return Collections.emptyMap();
                    })
            );
        }
        if (authenticationMethod == AuthenticationMethod.SSL) {
            properties.putAll(
                    sslAuthenticationProperties.build().properties().entrySet().stream()
                            .map(entry -> Map.entry(SchemaRegistryClientConfig.CLIENT_NAMESPACE + entry.getKey(), entry.getValue()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            );
        }
        properties.put(SchemaRegistryClientConfig.MISSING_CACHE_SIZE_CONFIG, 256);
        properties.put(SchemaRegistryClientConfig.MISSING_ID_CACHE_TTL_CONFIG, 60 * 1000L);
        properties.put(SchemaRegistryClientConfig.MISSING_SCHEMA_CACHE_TTL_CONFIG, 60 * 1000L);
        if (additionalProperties != null) {
            properties.putAll(additionalProperties);
        }
        return properties;
    }
}
