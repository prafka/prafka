package com.prafka.core.model;

import lombok.Getter;
import org.apache.kafka.clients.admin.ConfigEntry;

/**
 * Represents a configuration entry for a Kafka resource (broker, topic, etc.).
 *
 * <p>Contains the configuration name, current value, default value, data type,
 * source of the configuration, and documentation.
 *
 * @see ConfigEntry
 */
@Getter
public class Config {

    private final String name;
    private final String value;
    private final String defaultValue;
    private final ConfigEntry.ConfigType dataType;
    private final ConfigEntry.ConfigSource sourceType;
    private final String documentation;

    public Config(ConfigEntry source) {
        name = source.name();
        value = source.value();
        defaultValue = source.synonyms().stream()
                .filter(it -> it.source() == ConfigEntry.ConfigSource.DEFAULT_CONFIG)
                .findFirst()
                .map(ConfigEntry.ConfigSynonym::value)
                .orElse(null);
        dataType = source.type();
        sourceType = source.source();
        documentation = source.documentation();
    }
}
