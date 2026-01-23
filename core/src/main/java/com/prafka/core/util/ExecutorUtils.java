package com.prafka.core.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for working with {@link ExecutorService} instances.
 *
 * <p>Provides methods for graceful shutdown of executor services with timeout handling.
 */
public class ExecutorUtils {

    public static void closeExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
