package com.prafka.desktop.concurrent;

import com.prafka.desktop.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ServiceAdapterTest {

    @Test
    void shouldTaskWithSupplierReturnServiceAdapter() {
        // When
        var adapter = ServiceAdapter.task(() -> "result");

        // Then
        assertNotNull(adapter);
        assertTrue(adapter instanceof ServiceAdapter);
    }

    @Test
    void shouldTaskWithRunnableReturnServiceAdapter() {
        // When
        var adapter = ServiceAdapter.task(() -> {});

        // Then
        assertNotNull(adapter);
        assertTrue(adapter instanceof ServiceAdapter);
    }

    @Test
    void shouldOnSuccessReturnSameInstance() {
        // Given
        var adapter = ServiceAdapter.task(() -> "result");

        // When
        var result = adapter.onSuccess(value -> {});

        // Then
        assertSame(adapter, result);
    }

    @Test
    void shouldOnErrorReturnSameInstance() {
        // Given
        var adapter = ServiceAdapter.task(() -> "result");

        // When
        var result = adapter.onError(error -> {});

        // Then
        assertSame(adapter, result);
    }

    @Test
    void shouldFluentApiChaining() {
        // When
        var adapter = ServiceAdapter.task(() -> "result")
                .onSuccess(value -> {})
                .onError(error -> {});

        // Then
        assertNotNull(adapter);
    }

    @Test
    void shouldCreateTaskReturnNonNullTask() {
        // Given
        var adapter = ServiceAdapter.task(() -> "test-value");

        // When
        var task = adapter.createTask();

        // Then
        assertNotNull(task);
    }

    @Test
    void shouldTaskSupplierBeExecuted() throws Exception {
        // Given
        var executed = new AtomicBoolean(false);
        var adapter = ServiceAdapter.task(() -> {
            executed.set(true);
            return "done";
        });

        // When
        var task = adapter.createTask();
        var result = TestUtils.invokeCall(task);

        // Then
        assertTrue(executed.get());
        assertEquals("done", result);
    }

    @Test
    void shouldTaskRunnableBeExecuted() throws Exception {
        // Given
        var executed = new AtomicBoolean(false);
        var adapter = ServiceAdapter.task(() -> {
            executed.set(true);
        });

        // When
        var task = adapter.createTask();
        var result = TestUtils.invokeCall(task);

        // Then
        assertTrue(executed.get());
        assertNull(result);
    }

    @Test
    void shouldTaskReturnValueFromSupplier() throws Exception {
        // Given
        var adapter = ServiceAdapter.task(() -> 42);

        // When
        var task = adapter.createTask();
        var result = TestUtils.invokeCall(task);

        // Then
        assertEquals(42, result);
    }

    @Test
    void shouldTaskPropagateException() {
        // Given
        var adapter = ServiceAdapter.task(() -> {
            throw new RuntimeException("Test exception");
        });

        // When
        var task = adapter.createTask();

        // Then
        assertThrows(RuntimeException.class, () -> TestUtils.invokeCall(task));
    }
}
