package com.prafka.core.connection;

import lombok.Builder;

import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

@Builder
public class TokenAuthenticationProperties {

    private char[] token;

    public Properties properties(Function<char[], Map<String, String>> function) {
        var properties = new Properties();
        properties.putAll(function.apply(token));
        return properties;
    }
}
