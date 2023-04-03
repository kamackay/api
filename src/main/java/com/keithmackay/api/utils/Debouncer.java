package com.keithmackay.api.utils;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Debouncer<T> {
    private final ConcurrentHashMap<T, TimerTask> delayedMap = new ConcurrentHashMap<T, TimerTask>();
    private final long interval;

    public Debouncer(final Duration interval) {
        this.interval = interval.toMillis();
    }

    public void call(final T key, final Runnable action) {
        if (!delayedMap.contains(key) || delayedMap.get(key).isExpired(interval)) {
            TimerTask task = new TimerTask(key);
            delayedMap.put(key, task);
            action.run();
        }
    }

    @lombok.Data
    private class TimerTask {
        private final T key;
        private final long startTime;

        TimerTask(final T key) {
            this.key = key;
            this.startTime = System.currentTimeMillis();
        }

        public boolean isExpired(final long interval) {
            return System.currentTimeMillis() >= interval + startTime;
        }
    }
}
