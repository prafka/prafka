package com.prafka.core.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

/**
 * Represents a Kafka quota configuration.
 *
 * <p>Quotas allow limiting resource usage for users, client IDs, or IP addresses.
 * This includes producer/consumer byte rates, connection rates, and request percentages.
 */
@Getter
public class Quota {

    private final Entity entity;
    private final Config config;

    public Quota(Map.Entry<String, String> entity, Map.Entry<String, Double> config) {
        this.entity = new Entity(entity);
        this.config = new Config(config);
    }

    @Getter
    public static class Entity {

        private final String type;
        private final String name;
        private final String nameFormatted;
        private final EntityType internalType;
        private final boolean _default;

        public Entity(Map.Entry<String, String> entity) {
            type = entity.getKey();
            name = entity.getValue();
            nameFormatted = name == null ? "<default>" : name;
            internalType = EntityType.of(type);
            _default = name == null;
        }
    }

    @Getter
    public static class Config {

        private final String name;
        private final BigDecimal value;
        private final ConfigType internalType;

        public Config(Map.Entry<String, Double> config) {
            name = config.getKey();
            internalType = ConfigType.of(name);
            value = config.getValue() == null ? null : BigDecimal.valueOf(config.getValue());
        }
    }

    @Getter
    @RequiredArgsConstructor
    public enum EntityType {

        USER("user"),
        CLIENT_ID("client-id"),
        IP("ip"),
        UNKNOWN("unknown");

        private final String value;

        public static EntityType of(String value) {
            return Arrays.stream(values()).filter(it -> it.value.equals(value)).findFirst().orElse(EntityType.UNKNOWN);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public enum ConfigType {

        PRODUCER_RATE("producer_byte_rate"),
        CONSUMER_RATE("consumer_byte_rate"),
        CONNECTION_RATE("connection_creation_rate"),
        CONTROLLER_RATE("controller_mutation_rate"),
        REQUEST_PERCENTAGE("request_percentage"),
        UNKNOWN("unknown");

        private final String value;

        public static ConfigType of(String value) {
            return Arrays.stream(values()).filter(it -> it.value.equals(value)).findFirst().orElse(ConfigType.UNKNOWN);
        }
    }
}
