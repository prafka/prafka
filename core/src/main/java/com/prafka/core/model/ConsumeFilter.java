package com.prafka.core.model;

import java.util.List;
import java.util.Optional;

/**
 * Filter configuration for consuming records from a Kafka topic.
 *
 * <p>Defines the starting position, maximum results, partition selection, serialization types
 * for key and value, and optional filter expressions to apply during consumption.
 *
 * @see SerdeType
 */
public record ConsumeFilter(From from, int maxResults, List<Integer> partitions, SerdeType keySerde,
                            SerdeType valueSerde, List<Expression> expressions) {

    public record From(Type type, Optional<Long> offset, Optional<Long> timestamp) {

        public enum Type {
            BEGIN,     // show old messages first
            END,       // show new messages first
            OFFSET,    // show messages from specific offset
            DATETIME,  // show messages from specific date
            TIMESTAMP, // show messages from specific timestamp
        }
    }

    public record Expression(String name, String code, boolean isActive) {
    }
}
