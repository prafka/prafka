package com.prafka.core.connection;

import com.prafka.core.util.JaasUtils;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;

import java.util.Properties;

/**
 * Configuration properties for SASL authentication with Kafka.
 *
 * <p>This class encapsulates the SASL authentication settings including the security protocol,
 * mechanism, and credentials. It supports PLAIN, SCRAM-SHA-256, and SCRAM-SHA-512 mechanisms.
 *
 * <p>Use the Lombok-generated builder to create instances:
 * <pre>{@code
 * SaslAuthenticationProperties props = SaslAuthenticationProperties.builder()
 *     .securityProtocol(SaslSecurityProtocol.SASL_SSL)
 *     .mechanism(SaslMechanism.SCRAM_SHA_512)
 *     .username("user")
 *     .password("secret".toCharArray())
 *     .build();
 * }</pre>
 *
 * @see KafkaProperties
 * @see SaslMechanism
 * @see SaslSecurityProtocol
 * @see AuthenticationMethod#SASL
 */
@Builder
public class SaslAuthenticationProperties {

    private SaslSecurityProtocol securityProtocol;
    private SaslMechanism mechanism;
    private String username;
    private char[] password;

    public Properties properties() {
        var properties = new Properties();
        properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol.name());
        properties.put(SaslConfigs.SASL_MECHANISM, mechanism.getValue());
        if (mechanism == SaslMechanism.PLAIN) {
            if (StringUtils.isNotBlank(username) && password != null && password.length > 0) {
                properties.put(SaslConfigs.SASL_JAAS_CONFIG, JaasUtils.getSaslJaasPlainConfig(username, password));
            }
        }
        if (mechanism == SaslMechanism.SCRAM_SHA_256 || mechanism == SaslMechanism.SCRAM_SHA_512) {
            if (StringUtils.isNotBlank(username) && password != null && password.length > 0) {
                properties.put(SaslConfigs.SASL_JAAS_CONFIG, JaasUtils.getSaslJaasScramConfig(username, password));
            }
        }
        return properties;
    }
}
