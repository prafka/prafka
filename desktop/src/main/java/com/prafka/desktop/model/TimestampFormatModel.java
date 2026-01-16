package com.prafka.desktop.model;

import lombok.Getter;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Getter
public enum TimestampFormatModel {

    YYYY_MM_DD("yyyymmdd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss.SSS"),
    DD_MM_YYYY("ddmmyyyy", "dd.MM.yyyy HH:mm:ss", "dd.MM.yyyy HH:mm:ss.SSS");

    private final String code;
    private final String shortPattern;
    private final String fullPattern;
    private final DateTimeFormatter shortFormatter;
    private final DateTimeFormatter fullFormatter;

    TimestampFormatModel(String code, String shortPattern, String fullPattern) {
        this.code = code;
        this.shortPattern = shortPattern;
        this.fullPattern = fullPattern;
        shortFormatter = DateTimeFormatter.ofPattern(shortPattern).withZone(ZoneOffset.UTC);
        fullFormatter = DateTimeFormatter.ofPattern(fullPattern).withZone(ZoneOffset.UTC);
    }

    public static TimestampFormatModel getByCode(String code) {
        return switch (code) {
            case "yyyymmdd" -> YYYY_MM_DD;
            case "ddmmyyyy" -> DD_MM_YYYY;
            default -> throw new IllegalArgumentException();
        };
    }
}
