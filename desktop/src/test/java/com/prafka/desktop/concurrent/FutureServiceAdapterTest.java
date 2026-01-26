package com.prafka.desktop.concurrent;

import com.prafka.desktop.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class FutureServiceAdapterTest {

    @Test
    void shouldFutureTaskWithSupplierReturnAdapter() {
        // When
        var adapter = FutureServiceAdapter.futureTask(() -> CompletableFuture.completedFuture("result"));

        // Then
        assertNotNull(adapter);
        assertTrue(adapter instanceof FutureServiceAdapter);
    }

    @Test
    void shouldFutureTaskWithCancelFlagReturnAdapter() {
        // Given
        var cancelFlag = new AtomicBoolean(false);

        // When
        var adapter = FutureServiceAdapter.futureTask(
                () -> CompletableFuture.completedFuture("result"),
                cancelFlag
        );

        // Then
        assertNotNull(adapter);
    }

    @Test
    void shouldOnSuccessReturnSameInstance() {
        // Given
        var adapter = FutureServiceAdapter.futureTask(() -> CompletableFuture.completedFuture("result"));

        // When
        var result = adapter.onSuccess(value -> {});

        // Then
        assertSame(adapter, result);
    }

    @Test
    void shouldOnErrorReturnSameInstance() {
        // Given
        var adapter = FutureServiceAdapter.futureTask(() -> CompletableFuture.completedFuture("result"));

        // When
        var result = adapter.onError(error -> {});

        // Then
        assertSame(adapter, result);
    }

    @Test
    void shouldFluentApiChaining() {
        // When
        var adapter = FutureServiceAdapter.futureTask(() -> CompletableFuture.completedFuture("result"))
                .onSuccess(value -> {})
                .onError(error -> {});

        // Then
        assertNotNull(adapter);
    }

    @Test
    void shouldCreateTaskReturnNonNullTask() {
        // Given
        var adapter = FutureServiceAdapter.futureTask(() -> CompletableFuture.completedFuture("result"));

        // When
        var task = adapter.createTask();

        // Then
        assertNotNull(task);
    }

    @Test
    void shouldTaskWaitForCompletionStage() throws Exception {
        // Given
        var adapter = FutureServiceAdapter.futureTask(() -> CompletableFuture.completedFuture("completed-value"));

        // When
        var task = adapter.createTask();
        var result = TestUtils.invokeCall(task);

        // Then
        assertEquals("completed-value", result);
    }

    @Test
    void shouldTaskHandleFailedFuture() {
        // Given
        var adapter = FutureServiceAdapter.futureTask(() -> {
            var future = new CompletableFuture<String>();
            future.completeExceptionally(new RuntimeException("Future failed"));
            return future;
        });

        // When
        var task = adapter.createTask();

        // Then
        assertThrows(Exception.class, () -> TestUtils.invokeCall(task));
    }

    @Test
    void shouldCancelSetAtomicBooleanFlag() {
        // Given
        var cancelFlag = new AtomicBoolean(false);
        var adapter = FutureServiceAdapter.futureTask(
                () -> CompletableFuture.completedFuture("result"),
                cancelFlag
        );

        // When
        adapter.cancel();

        // Then
        assertTrue(cancelFlag.get());
    }

    @Test
    void shouldCancelWithNullFlagNotThrow() {
        // Given
        var adapter = FutureServiceAdapter.futureTask(() -> CompletableFuture.completedFuture("result"));

        // When/Then
        assertDoesNotThrow(adapter::cancel);
    }

    @Test
    void shouldTaskSupplierBeExecuted() throws Exception {
        // Given
        var executed = new AtomicBoolean(false);
        var adapter = FutureServiceAdapter.futureTask(() -> {
            executed.set(true);
            return CompletableFuture.completedFuture("done");
        });

        // When
        var task = adapter.createTask();
        TestUtils.invokeCall(task);

        // Then
        assertTrue(executed.get());
    }
}
