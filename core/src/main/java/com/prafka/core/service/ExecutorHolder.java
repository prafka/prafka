package com.prafka.core.service;

import com.prafka.core.util.ExecutorUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Holds shared thread pool executors for asynchronous service operations.
 *
 * <p>Provides dedicated thread pools for Schema Registry and Kafka Connect operations
 * to prevent blocking the main thread during HTTP-based client calls.
 */
public class ExecutorHolder {

    public static final ExecutorService schemaRegistryExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    public static final ExecutorService connectExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public static void close() {
        ExecutorUtils.closeExecutor(schemaRegistryExecutor);
        ExecutorUtils.closeExecutor(connectExecutor);
    }
}
