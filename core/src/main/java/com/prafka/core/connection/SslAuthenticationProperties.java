package com.prafka.core.connection;

import lombok.Builder;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;

import java.util.Properties;

@Builder
public class SslAuthenticationProperties {

    private String keystoreLocation;
    private String keystorePassword;
    private String keyPassword;
    private String truststoreLocation;
    private String truststorePassword;

    public Properties properties() {
        var properties = new Properties();
        properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name());
        if (StringUtils.isNotBlank(keystoreLocation)) {
            properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystoreLocation);
        }
        if (StringUtils.isNotBlank(keystorePassword)) {
            properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keystorePassword);
        }
        if (StringUtils.isNotBlank(keyPassword)) {
            properties.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, keyPassword);
        }
        if (StringUtils.isNotBlank(truststoreLocation)) {
            properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststoreLocation);
        }
        if (StringUtils.isNotBlank(truststorePassword)) {
            properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword);
        }
        return properties;
    }
}
