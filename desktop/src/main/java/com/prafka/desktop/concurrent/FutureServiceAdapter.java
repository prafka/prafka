package com.prafka.desktop.concurrent;

import com.prafka.desktop.service.ExecutorHolder;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.commons.lang3.function.FailableSupplier;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * JavaFX Service adapter for wrapping CompletionStage-based async operations.
 *
 * <p>Provides fluent API for success/error handlers and integrates cancellation
 * with both the service and the underlying future.
 */
public class FutureServiceAdapter<T> extends Service<T> {

    private final FailableSupplier<CompletionStage<T>, Exception> supplier;
    private final AtomicBoolean cancel;
    private CompletionStage<T> future;

    private FutureServiceAdapter(FailableSupplier<CompletionStage<T>, Exception> supplier, AtomicBoolean cancel) {
        this.supplier = supplier;
        this.cancel = cancel;
        setExecutor(ExecutorHolder.taskExecutor);
    }

    public FutureServiceAdapter<T> onSuccess(Consumer<T> consumer) {
        //noinspection unchecked
        setOnSucceeded(event -> consumer.accept((T) event.getSource().getValue()));
        return this;
    }

    public FutureServiceAdapter<T> onError(Consumer<Throwable> consumer) {
        setOnFailed(event -> consumer.accept(event.getSource().getException()));
        return this;
    }

    public FutureServiceAdapter<T> startNow() {
        super.start();
        return this;
    }

    @Override
    public boolean cancel() {
        var result = super.cancel();
        if (future != null) {
            result = future.toCompletableFuture().cancel(true);
        }
        if (cancel != null) {
            cancel.set(true);
        }
        return result;
    }

    @Override
    protected Task<T> createTask() {
        return new Task<>() {
            @Override
            protected T call() throws Exception {
                future = supplier.get();
                return future.toCompletableFuture().get();
            }
        };
    }

    public static <T> FutureServiceAdapter<T> futureTask(FailableSupplier<CompletionStage<T>, Exception> supplier) {
        return new FutureServiceAdapter<>(supplier, null);
    }

    public static <T> FutureServiceAdapter<T> futureTask(FailableSupplier<CompletionStage<T>, Exception> supplier, AtomicBoolean cancel) {
        return new FutureServiceAdapter<>(supplier, cancel);
    }
}
