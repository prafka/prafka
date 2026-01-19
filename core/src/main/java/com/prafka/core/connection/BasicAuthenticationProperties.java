package com.prafka.core.connection;

import lombok.Builder;

import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;

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
