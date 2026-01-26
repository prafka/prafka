package com.prafka.desktop.concurrent;

import com.prafka.desktop.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ScheduledServiceAdapterTest {

    @Test
    void shouldScheduleTaskWithSupplierReturnAdapter() {
        // When
        var adapter = ScheduledServiceAdapter.scheduleTask(() -> "result");

        // Then
        assertNotNull(adapter);
        assertTrue(adapter instanceof ScheduledServiceAdapter);
    }

    @Test
    void shouldScheduleTaskWithRunnableReturnAdapter() {
        // When
        var adapter = ScheduledServiceAdapter.scheduleTask(() -> {});

        // Then
        assertNotNull(adapter);
        assertTrue(adapter instanceof ScheduledServiceAdapter);
    }

    @Test
    void shouldOnSuccessReturnSameInstance() {
        // Given
        var adapter = ScheduledServiceAdapter.scheduleTask(() -> "result");

        // When
        var result = adapter.onSuccess(value -> {});

        // Then
        assertSame(adapter, result);
    }

    @Test
    void shouldOnErrorReturnSameInstance() {
        // Given
        var adapter = ScheduledServiceAdapter.scheduleTask(() -> "result");

        // When
        var result = adapter.onError(error -> {});

        // Then
        assertSame(adapter, result);
    }

    @Test
    void shouldFluentApiChaining() {
        // When
        var adapter = ScheduledServiceAdapter.scheduleTask(() -> "result")
                .onSuccess(value -> {})
                .onError(error -> {});

        // Then
        assertNotNull(adapter);
    }

    @Test
    void shouldCreateTaskReturnNonNullTask() {
        // Given
        var adapter = ScheduledServiceAdapter.scheduleTask(() -> "test-value");

        // When
        var task = adapter.createTask();

        // Then
        assertNotNull(task);
    }

    @Test
    void shouldTaskSupplierBeExecuted() throws Exception {
        // Given
        var executed = new AtomicBoolean(false);
        var adapter = ScheduledServiceAdapter.scheduleTask(() -> {
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
        var adapter = ScheduledServiceAdapter.scheduleTask(() -> {
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
        var adapter = ScheduledServiceAdapter.scheduleTask(() -> 42);

        // When
        var task = adapter.createTask();
        var result = TestUtils.invokeCall(task);

        // Then
        assertEquals(42, result);
    }

    @Test
    void shouldTaskPropagateException() {
        // Given
        var adapter = ScheduledServiceAdapter.scheduleTask(() -> {
            throw new RuntimeException("Test exception");
        });

        // When
        var task = adapter.createTask();

        // Then
        assertThrows(RuntimeException.class, () -> TestUtils.invokeCall(task));
    }

    @Test
    void shouldConstructorAcceptSupplier() {
        // When
        var adapter = new ScheduledServiceAdapter<>(() -> "value");

        // Then
        assertNotNull(adapter);
    }

    @Test
    void shouldTaskExecuteWithoutMaxCount() throws Exception {
        // Given
        var counter = new AtomicInteger(0);
        var adapter = ScheduledServiceAdapter.scheduleTask(() -> {
            counter.incrementAndGet();
            return counter.get();
        });

        // When - execute multiple tasks
        var task1 = adapter.createTask();
        TestUtils.invokeCall(task1);
        var task2 = adapter.createTask();
        TestUtils.invokeCall(task2);
        var task3 = adapter.createTask();
        TestUtils.invokeCall(task3);

        // Then - all should execute
        assertEquals(3, counter.get());
    }
}
