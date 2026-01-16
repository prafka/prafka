package com.prafka.core.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Getter
@Setter
public class NewRecord {

    private String key;
    private SerdeType keySerde = SerdeType.STRING;
    private Optional<String> keySchemaSubject = Optional.empty();
    private String value;
    private SerdeType valueSerde = SerdeType.STRING;
    private Optional<String> valueSchemaSubject = Optional.empty();
    private Map<String, String> headers = Collections.emptyMap();
    private Optional<Integer> partition = Optional.empty();
    private Optional<Long> timestamp = Optional.empty();
    private CompressionType compression = CompressionType.NONE;
    private Asks asks = Asks.ALL;
    private boolean idempotence = true;

    @Getter
    @RequiredArgsConstructor
    public enum CompressionType {

        NONE("none"),
        GZIP("gzip"),
        SNAPPY("snappy"),
        LZ4("lz4"),
        ZSTD("zstd");

        private final String value;
    }

    @Getter
    @RequiredArgsConstructor
    public enum Asks {

        NONE("0"),
        LEADER("1"),
        ALL("all");

        private final String value;
    }
}
