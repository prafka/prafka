package com.prafka.desktop.concurrent;

import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.util.Duration;
import org.apache.commons.lang3.function.FailableRunnable;
import org.apache.commons.lang3.function.FailableSupplier;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.prafka.desktop.service.ExecutorHolder.taskExecutor;

/**
 * JavaFX ScheduledService adapter for periodic background task execution.
 *
 * <p>Supports configurable delay, period, and optional maximum execution count
 * with fluent API for callbacks.
 */
public class ScheduledServiceAdapter<T> extends ScheduledService<T> {

    private final FailableSupplier<T, Exception> supplier;
    private int maxExecCount = 0;
    private final AtomicInteger curExecCount = new AtomicInteger(0);

    public ScheduledServiceAdapter(FailableSupplier<T, Exception> supplier) {
        this.supplier = supplier;
        setExecutor(taskExecutor);
    }

    @Override
    protected Task<T> createTask() {
        return new Task<>() {
            @Override
            protected T call() throws Exception {
                if (maxExecCount > 0) {
                    if (curExecCount.getAndIncrement() < maxExecCount) {
                        return supplier.get();
                    } else {
                        cancel();
                        return null;
                    }
                } else {
                    return supplier.get();
                }
            }
        };
    }

    public static <T> ScheduledServiceAdapter<T> scheduleTask(FailableSupplier<T, Exception> supplier) {
        return new ScheduledServiceAdapter<>(supplier);
    }

    public static ScheduledServiceAdapter<Void> scheduleTask(FailableRunnable<Exception> runnable) {
        return new ScheduledServiceAdapter<>(() -> {
            runnable.run();
            return null;
        });
    }

    public ScheduledServiceAdapter<T> onSuccess(Consumer<T> consumer) {
        //noinspection unchecked
        setOnSucceeded(event -> consumer.accept((T) event.getSource().getValue()));
        return this;
    }

    public ScheduledServiceAdapter<T> onError(Consumer<Throwable> consumer) {
        setOnFailed(event -> consumer.accept(event.getSource().getException()));
        return this;
    }

    public ScheduledServiceAdapter<T> start(Duration period) {
        setPeriod(period);
        start();
        return this;
    }

    public ScheduledServiceAdapter<T> start(Duration delay, Duration period) {
        setDelay(delay);
        setPeriod(period);
        start();
        return this;
    }

    public ScheduledServiceAdapter<T> start(Duration period, int count) {
        setPeriod(period);
        maxExecCount = count;
        start();
        return this;
    }
}
