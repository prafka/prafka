package com.prafka.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormatUtilsTest {

    @Test
    void shouldReturnEmptyStringForBlankValue() {
        assertEquals("", FormatUtils.prettyConfigValue("any.name", ""));
        assertEquals("", FormatUtils.prettyConfigValue("any.name", "   "));
        assertEquals("", FormatUtils.prettyConfigValue("any.name", null));
    }

    @Test
    void shouldReturnInfinityForLongMaxValue() {
        assertEquals("Infinity", FormatUtils.prettyConfigValue("any.name", String.valueOf(Long.MAX_VALUE)));
    }

    @Test
    void shouldFormatMillisecondsSuffix() {
        assertEquals("1s", FormatUtils.prettyConfigValue("retention.ms", "1000"));
        assertEquals("1min", FormatUtils.prettyConfigValue("retention.ms", "60000"));
        assertEquals("1h", FormatUtils.prettyConfigValue("session.timeout.ms", "3600000"));
    }

    @Test
    void shouldReturnOriginalValueForNegativeMs() {
        assertEquals("-1", FormatUtils.prettyConfigValue("retention.ms", "-1"));
        assertEquals("-100", FormatUtils.prettyConfigValue("session.timeout.ms", "-100"));
    }

    @Test
    void shouldReturnOriginalValueWhenMsParsingFails() {
        assertEquals("not-a-number", FormatUtils.prettyConfigValue("retention.ms", "not-a-number"));
    }

    @Test
    void shouldFormatBytesSuffix() {
        assertEquals("1 KB", FormatUtils.prettyConfigValue("segment.bytes", "1024"));
        assertEquals("1 MB", FormatUtils.prettyConfigValue("log.segment.bytes", "1048576"));
        assertEquals("1 GB", FormatUtils.prettyConfigValue("max.message.bytes", "1073741824"));
    }

    @Test
    void shouldReturnOriginalValueForNegativeBytes() {
        assertEquals("-1", FormatUtils.prettyConfigValue("segment.bytes", "-1"));
    }

    @Test
    void shouldReturnOriginalValueWhenBytesParsingFails() {
        assertEquals("not-a-number", FormatUtils.prettyConfigValue("segment.bytes", "not-a-number"));
    }

    @Test
    void shouldReturnOriginalValueForNonSpecialSuffix() {
        assertEquals("somevalue", FormatUtils.prettyConfigValue("retention.time", "somevalue"));
        assertEquals("12345", FormatUtils.prettyConfigValue("some.config", "12345"));
    }

    @Test
    void shouldFormatDurationInMilliseconds() {
        assertEquals("1s", FormatUtils.prettyDurationInMs(1000L));
        assertEquals("1min", FormatUtils.prettyDurationInMs(60000L));
        assertEquals("1h", FormatUtils.prettyDurationInMs(3600000L));
        assertEquals("1d", FormatUtils.prettyDurationInMs(86400000L));
    }

    @Test
    void shouldFormatDurationWithMultipleUnits() {
        assertEquals("1h 30min", FormatUtils.prettyDurationInMs(5400000L));
        assertEquals("1d 1h 1min 1s 1ms", FormatUtils.prettyDurationInMs(90061001L));
    }

    @Test
    void shouldFormatDurationFromString() {
        assertEquals("1s", FormatUtils.prettyDurationInMs("1000"));
        assertEquals("1min", FormatUtils.prettyDurationInMs("60000"));
    }

    @Test
    void shouldFormatSizeInBytes() {
        assertEquals("0 bytes", FormatUtils.prettySizeInBytes(0L));
        assertEquals("512 bytes", FormatUtils.prettySizeInBytes(512L));
        assertEquals("1 KB", FormatUtils.prettySizeInBytes(1024L));
        assertEquals("1 MB", FormatUtils.prettySizeInBytes(1048576L));
        assertEquals("1 GB", FormatUtils.prettySizeInBytes(1073741824L));
    }

    @Test
    void shouldFormatSizeInBytesFromString() {
        assertEquals("1 KB", FormatUtils.prettySizeInBytes("1024"));
        assertEquals("1 MB", FormatUtils.prettySizeInBytes("1048576"));
    }
}
