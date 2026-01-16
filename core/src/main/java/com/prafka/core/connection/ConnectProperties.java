package com.prafka.core.connection;

import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

@Builder
public class ConnectProperties {

    private String url;
    private AuthenticationMethod authenticationMethod;
    private BasicAuthenticationProperties.Builder basicAuthenticationProperties;
    private SslAuthenticationProperties.Builder sslAuthenticationProperties;
    private Map<String, String> additionalProperties;

    public Properties properties() {
        var properties = new Properties();
        properties.put("url", url);
        if (authenticationMethod == AuthenticationMethod.BASIC) {
            properties.putAll(
                    basicAuthenticationProperties.build().properties((username, password) -> {
                        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
                            return Map.of("basic.username", username, "basic.password", password);
                        }
                        return Collections.emptyMap();
                    })
            );
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
