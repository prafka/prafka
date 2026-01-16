package com.prafka.desktop.util;

import com.prafka.core.model.SerdeType;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormatUtilsTest {

    @Test
    void shouldPrettyDurationInMs() {
        assertEquals("500ms", FormatUtils.prettyDurationInMs(500L));
        assertEquals("5s", FormatUtils.prettyDurationInMs(5000L));
        assertEquals("5s 500ms", FormatUtils.prettyDurationInMs(5500L));
        assertEquals("5min", FormatUtils.prettyDurationInMs(300000L));
        assertEquals("5min 30s", FormatUtils.prettyDurationInMs(330000L));
        assertEquals("2h", FormatUtils.prettyDurationInMs(7200000L));
        assertEquals("2h 30min", FormatUtils.prettyDurationInMs(9000000L));
        assertEquals("3d", FormatUtils.prettyDurationInMs(259200000L));
        assertEquals("2d 3h 4min 5s 600ms", FormatUtils.prettyDurationInMs(183845600L));
    }

    @Test
    void shouldPrettyEnum() {
        assertEquals("Schema registry", FormatUtils.prettyEnum(SerdeType.SCHEMA_REGISTRY));
    }

    @Test
    void shouldSplitAclPrincipal() {
        assertEquals(Pair.of("user", "user"), FormatUtils.splitAclPrincipal("user:user"));
    }
}