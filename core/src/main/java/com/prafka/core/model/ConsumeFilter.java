package com.prafka.core.model;

import java.util.List;
import java.util.Optional;

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
