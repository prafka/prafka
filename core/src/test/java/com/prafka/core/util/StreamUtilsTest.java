package com.prafka.core.util;

import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class StreamUtilsTest {

    @Test
    void shouldMapKafkaFutureToListOnSuccess() throws ExecutionException, InterruptedException {
        var future = KafkaFuture.completedFuture("test-value");

        var result = StreamUtils.mapKafkaFutureToList(future).get();

        assertEquals(1, result.size());
        assertEquals("test-value", result.getFirst());
    }

    @Test
    void shouldMapKafkaFutureToEmptyListOnException() throws ExecutionException, InterruptedException {
        var future = new KafkaFutureImpl<String>();
        future.completeExceptionally(new RuntimeException("Test exception"));

        var result = StreamUtils.mapKafkaFutureToList(future).get();

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldCombineFutureLists() throws ExecutionException, InterruptedException {
        var future1 = CompletableFuture.completedFuture(List.of("a", "b"));
        var future2 = CompletableFuture.completedFuture(List.of("c", "d"));

        var combiner = StreamUtils.<String>combineFutureList();
        var result = combiner.apply(future1, future2).get();

        assertEquals(4, result.size());
        assertTrue(result.containsAll(List.of("a", "b", "c", "d")));
    }

    @Test
    void shouldCombineEmptyFutureLists() throws ExecutionException, InterruptedException {
        var future1 = CompletableFuture.completedFuture(Collections.<String>emptyList());
        var future2 = CompletableFuture.completedFuture(List.of("a"));

        var combiner = StreamUtils.<String>combineFutureList();
        var result = combiner.apply(future1, future2).get();

        assertEquals(1, result.size());
        assertEquals("a", result.getFirst());
    }

    @Test
    void shouldReturnCompletedFutureWithEmptyList() throws ExecutionException, InterruptedException {
        var supplier = StreamUtils.<String>completeFutureEmptyList();
        var result = supplier.get().get();

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldTryReturnValue() {
        var result = StreamUtils.tryReturn(() -> "success");

        assertEquals("success", result);
    }

    @Test
    void shouldTryReturnWrapCheckedException() {
        var exception = assertThrows(RuntimeException.class, () ->
                StreamUtils.tryReturn(() -> {
                    throw new Exception("Checked exception");
                })
        );

        assertEquals("Checked exception", exception.getCause().getMessage());
    }

    @Test
    void shouldTryVoidExecuteSuccessfully() {
        var executed = new boolean[]{false};

        StreamUtils.tryVoid(() -> executed[0] = true);

        assertTrue(executed[0]);
    }

    @Test
    void shouldTryVoidWrapCheckedException() {
        var exception = assertThrows(RuntimeException.class, () ->
                StreamUtils.tryVoid(() -> {
                    throw new Exception("Checked exception");
                })
        );

        assertEquals("Checked exception", exception.getCause().getMessage());
    }

    @Test
    void shouldTryIgnoreExecuteSuccessfully() {
        var executed = new boolean[]{false};

        StreamUtils.tryIgnore(() -> executed[0] = true);

        assertTrue(executed[0]);
    }

    @Test
    void shouldTryIgnoreSuppressException() {
        assertDoesNotThrow(() ->
                StreamUtils.tryIgnore(() -> {
                    throw new Exception("Should be ignored");
                })
        );
    }

    @Test
    void shouldTryOrEmptyReturnPresentOptional() {
        var result = StreamUtils.tryOrEmpty(() -> "value");

        assertTrue(result.isPresent());
        assertEquals("value", result.get());
    }

    @Test
    void shouldTryOrEmptyReturnEmptyOnException() {
        var result = StreamUtils.tryOrEmpty(() -> {
            throw new Exception("Test exception");
        });

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldTryOrEmptyReturnEmptyForNullValue() {
        var result = StreamUtils.tryOrEmpty(() -> null);

        assertTrue(result.isEmpty());
    }
}
