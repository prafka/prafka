package com.prafka.desktop.concurrent;

import javafx.concurrent.Service;
import org.apache.commons.lang3.function.FailableRunnable;
import org.apache.commons.lang3.function.FailableSupplier;

import java.util.function.Consumer;

import static com.prafka.desktop.service.ExecutorHolder.taskExecutor;

/**
 * JavaFX Service adapter for running background tasks with fluent callback configuration.
 *
 * <p>Simplifies creating background services with success and error handlers,
 * using the shared task executor pool.
 */
public class ServiceAdapter<T> extends Service<T> {

    private final FailableSupplier<T, Exception> supplier;

    private ServiceAdapter(FailableSupplier<T, Exception> supplier) {
        this.supplier = supplier;
        setExecutor(taskExecutor);
    }

    public ServiceAdapter<T> onSuccess(Consumer<T> consumer) {
        //noinspection unchecked
        setOnSucceeded(event -> consumer.accept((T) event.getSource().getValue()));
        return this;
    }

    public ServiceAdapter<T> onError(Consumer<Throwable> consumer) {
        setOnFailed(event -> consumer.accept(event.getSource().getException()));
        return this;
    }

    public ServiceAdapter<T> startNow() {
        super.start();
        return this;
    }

    @Override
    protected javafx.concurrent.Task<T> createTask() {
        return new javafx.concurrent.Task<>() {
            @Override
            protected T call() throws Exception {
                return supplier.get();
            }
        };
    }

    public static <T> ServiceAdapter<T> task(FailableSupplier<T, Exception> supplier) {
        return new ServiceAdapter<>(supplier);
    }

    public static ServiceAdapter<Void> task(FailableRunnable<Exception> runnable) {
        return new ServiceAdapter<>(() -> {
            runnable.run();
            return null;
        });
    }
}
