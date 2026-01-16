package com.prafka.core.util;

public class JaasUtils {

    private static final String SASL_JAAS_PLAIN_TEMPLATE = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";";
    private static final String SASL_JAAS_SCRAM_TEMPLATE = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";

    public static String getSaslJaasPlainConfig(String username, String password) {
        return SASL_JAAS_PLAIN_TEMPLATE.formatted(username, password);
    }

    public static String getSaslJaasScramConfig(String username, String password) {
        return SASL_JAAS_SCRAM_TEMPLATE.formatted(username, password);
    }
}
