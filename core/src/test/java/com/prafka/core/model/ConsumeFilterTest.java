package com.prafka.core.model;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ConsumeFilterTest {

    @Test
    void shouldCreateConsumeFilterWithBeginFrom() {
        var from = new ConsumeFilter.From(ConsumeFilter.From.Type.BEGIN, Optional.empty(), Optional.empty());
        var filter = new ConsumeFilter(from, 100, Collections.emptyList(), SerdeType.STRING, SerdeType.JSON, Collections.emptyList());

        assertEquals(ConsumeFilter.From.Type.BEGIN, filter.from().type());
        assertEquals(100, filter.maxResults());
        assertTrue(filter.from().offset().isEmpty());
        assertTrue(filter.from().timestamp().isEmpty());
    }

    @Test
    void shouldCreateConsumeFilterWithEndFrom() {
        var from = new ConsumeFilter.From(ConsumeFilter.From.Type.END, Optional.empty(), Optional.empty());
        var filter = new ConsumeFilter(from, 50, Collections.emptyList(), SerdeType.STRING, SerdeType.STRING, Collections.emptyList());

        assertEquals(ConsumeFilter.From.Type.END, filter.from().type());
        assertEquals(50, filter.maxResults());
    }

    @Test
    void shouldCreateConsumeFilterWithOffsetFrom() {
        var from = new ConsumeFilter.From(ConsumeFilter.From.Type.OFFSET, Optional.of(500L), Optional.empty());
        var filter = new ConsumeFilter(from, 100, Collections.emptyList(), SerdeType.STRING, SerdeType.STRING, Collections.emptyList());

        assertEquals(ConsumeFilter.From.Type.OFFSET, filter.from().type());
        assertTrue(filter.from().offset().isPresent());
        assertEquals(500L, filter.from().offset().get());
    }

    @Test
    void shouldCreateConsumeFilterWithTimestampFrom() {
        var timestamp = 1234567890000L;
        var from = new ConsumeFilter.From(ConsumeFilter.From.Type.TIMESTAMP, Optional.empty(), Optional.of(timestamp));
        var filter = new ConsumeFilter(from, 100, Collections.emptyList(), SerdeType.STRING, SerdeType.STRING, Collections.emptyList());

        assertEquals(ConsumeFilter.From.Type.TIMESTAMP, filter.from().type());
        assertTrue(filter.from().timestamp().isPresent());
        assertEquals(timestamp, filter.from().timestamp().get());
    }

    @Test
    void shouldCreateConsumeFilterWithDateTimeFrom() {
        var timestamp = 1234567890000L;
        var from = new ConsumeFilter.From(ConsumeFilter.From.Type.DATETIME, Optional.empty(), Optional.of(timestamp));
        var filter = new ConsumeFilter(from, 100, Collections.emptyList(), SerdeType.STRING, SerdeType.STRING, Collections.emptyList());

        assertEquals(ConsumeFilter.From.Type.DATETIME, filter.from().type());
        assertTrue(filter.from().timestamp().isPresent());
    }

    @Test
    void shouldCreateConsumeFilterWithSpecificPartitions() {
        var from = new ConsumeFilter.From(ConsumeFilter.From.Type.BEGIN, Optional.empty(), Optional.empty());
        var partitions = List.of(0, 1, 2);
        var filter = new ConsumeFilter(from, 100, partitions, SerdeType.STRING, SerdeType.STRING, Collections.emptyList());

        assertEquals(3, filter.partitions().size());
        assertTrue(filter.partitions().contains(0));
        assertTrue(filter.partitions().contains(1));
        assertTrue(filter.partitions().contains(2));
    }

    @Test
    void shouldCreateConsumeFilterWithSerdeTypes() {
        var from = new ConsumeFilter.From(ConsumeFilter.From.Type.BEGIN, Optional.empty(), Optional.empty());
        var filter = new ConsumeFilter(from, 100, Collections.emptyList(), SerdeType.LONG, SerdeType.AVRO, Collections.emptyList());

        assertEquals(SerdeType.LONG, filter.keySerde());
        assertEquals(SerdeType.AVRO, filter.valueSerde());
    }

    @Test
    void shouldCreateConsumeFilterWithExpressions() {
        var from = new ConsumeFilter.From(ConsumeFilter.From.Type.BEGIN, Optional.empty(), Optional.empty());
        var expression1 = new ConsumeFilter.Expression("filter1", "value.contains('test')", true);
        var expression2 = new ConsumeFilter.Expression("filter2", "key == 'abc'", false);
        var filter = new ConsumeFilter(from, 100, Collections.emptyList(), SerdeType.STRING, SerdeType.STRING, List.of(expression1, expression2));

        assertEquals(2, filter.expressions().size());
        assertEquals("filter1", filter.expressions().get(0).name());
        assertEquals("value.contains('test')", filter.expressions().get(0).code());
        assertTrue(filter.expressions().get(0).isActive());
        assertFalse(filter.expressions().get(1).isActive());
    }

    @Test
    void shouldCreateExpressionRecord() {
        var expression = new ConsumeFilter.Expression("myFilter", "partition == 0", true);

        assertEquals("myFilter", expression.name());
        assertEquals("partition == 0", expression.code());
        assertTrue(expression.isActive());
    }

    @Test
    void shouldCreateFromRecord() {
        var from = new ConsumeFilter.From(ConsumeFilter.From.Type.OFFSET, Optional.of(100L), Optional.of(9999L));

        assertEquals(ConsumeFilter.From.Type.OFFSET, from.type());
        assertEquals(100L, from.offset().get());
        assertEquals(9999L, from.timestamp().get());
    }

    @Test
    void shouldHaveAllFromTypes() {
        var types = ConsumeFilter.From.Type.values();

        assertEquals(5, types.length);
        assertNotNull(ConsumeFilter.From.Type.BEGIN);
        assertNotNull(ConsumeFilter.From.Type.END);
        assertNotNull(ConsumeFilter.From.Type.OFFSET);
        assertNotNull(ConsumeFilter.From.Type.DATETIME);
        assertNotNull(ConsumeFilter.From.Type.TIMESTAMP);
    }
}
