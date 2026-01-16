package com.prafka.core.connection;

import com.prafka.core.util.JaasUtils;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;

import java.util.Properties;

@Builder
public class SaslAuthenticationProperties {

    private SaslSecurityProtocol securityProtocol;
    private SaslMechanism mechanism;
    private String username;
    private String password;

    public Properties properties() {
        var properties = new Properties();
        properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol.name());
        properties.put(SaslConfigs.SASL_MECHANISM, mechanism.getValue());
        if (mechanism == SaslMechanism.PLAIN) {
            if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
                properties.put(SaslConfigs.SASL_JAAS_CONFIG, JaasUtils.getSaslJaasPlainConfig(username, password));
            }
        }
        if (mechanism == SaslMechanism.SCRAM_SHA_256 || mechanism == SaslMechanism.SCRAM_SHA_512) {
            if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
                properties.put(SaslConfigs.SASL_JAAS_CONFIG, JaasUtils.getSaslJaasScramConfig(username, password));
            }
        }
        return properties;
    }
}
