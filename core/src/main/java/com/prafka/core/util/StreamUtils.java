package com.prafka.core.util;

import org.apache.commons.lang3.function.FailableRunnable;
import org.apache.commons.lang3.function.FailableSupplier;
import org.apache.kafka.common.KafkaFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

public class StreamUtils {

    public static <T> CompletableFuture<List<T>> mapKafkaFutureToList(KafkaFuture<T> future) {
        return future.toCompletionStage().toCompletableFuture()
                .thenApply(Collections::singletonList)
                .exceptionally(e -> Collections.emptyList());
    }

    public static <T> BinaryOperator<CompletableFuture<List<T>>> combineFutureList() {
        return (f1, f2) ->
                f1.thenCombine(f2, (l1, l2) ->
                        new ArrayList<>(l1.size() + l2.size()) {{
                            addAll(l1);
                            addAll(l2);
                        }}
                );
    }

    public static <T> Supplier<CompletableFuture<List<T>>> completeFutureEmptyList() {
        return () -> CompletableFuture.completedFuture(Collections.emptyList());
    }

    public static <T> T tryReturn(FailableSupplier<T, Exception> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void tryVoid(FailableRunnable<Exception> runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void tryIgnore(FailableRunnable<Exception> runnable) {
        try {
            runnable.run();
        } catch (Exception ignored) {
        }
    }

    public static <T> Optional<T> tryOrEmpty(FailableSupplier<T, Exception> supplier) {
        try {
            return Optional.ofNullable(supplier.get());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
