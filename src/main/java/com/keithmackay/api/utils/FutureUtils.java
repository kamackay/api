package com.keithmackay.api.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.runAsync;

@Slf4j
public class FutureUtils {

    /**
     * Provide a list of Completable Futures, the result will be whichever is fastest, Excluding null responses
     * <p>
     * This is intended to be functionally identical to ES6's Promise.race
     *
     * @param futures - List of completable Futures with varying delays
     * @param <T>     - Type of Expected Return
     * @return - Completable Future containing first result
     */
    @SafeVarargs
    public static <T> CompletableFuture<T> fastest(final long timeout, CompletableFuture<T>... futures) {
        CompletableFuture<T> f = new CompletableFuture<>();
        doLater(() -> {
            if (!f.isDone()) {
                f.complete(null);
            }
        }, timeout);
        Arrays.stream(futures)
                .forEach(s -> s.thenAccept(r -> {
                    if (r != null) {
                        f.complete(r);
                        f.cancel(true);
                    }
                }));
        return f;
    }

    /**
     * Provide a list of Completable Futures, the result will be whichever is fastest.
     * <p>
     * This is intended to be functionally identical to ES6's Promise.race
     *
     * @param futures - List of completable Futures with varying delays
     * @param <T>     - Type of Expected Return
     * @return - Completable Future containing first result
     */
    @SafeVarargs
    public static <T> CompletableFuture<T> fastest(CompletableFuture<T>... futures) {
        CompletableFuture<T> f = new CompletableFuture<>();
        Arrays.stream(futures).forEach(s -> s.thenAccept(f::complete));
        return f;
    }

    public static void doLater(final Runnable task, final long timeout) {
        runAsync(() -> {
            try {
                Thread.sleep(timeout);
                task.run();
            } catch (Exception e) {
                log.error("Couldn't defer task", e);
            }
        });
    }

    private static <T> Supplier<T> returnValue(Callable<T> value) {
        return () -> {
            try {
                return value.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
