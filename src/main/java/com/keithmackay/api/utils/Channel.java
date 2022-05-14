package com.keithmackay.api.utils;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.util.BlockingArrayQueue;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class Channel<T> implements Closeable {

    private boolean isClosed;

    private final BlockingQueue<T> feed;

    public Channel() {
        this.isClosed = false;
        this.feed = new BlockingArrayQueue<>();
    }

    public void iterate(final Consumer<T> consumer) {
        while (!this.isClosed) {
            try {
                final T t = this.feed.poll(100, TimeUnit.MILLISECONDS);
                if (t == null) {
                    continue;
                }
                log.debug("Pulled Document from channel");
                consumer.accept(t);
            } catch (InterruptedException e) {
                log.warn("Interruption in Channel", e);
            }
        }
    }

    public void push(final T t) {
        this.feed.add(t);
    }

    @Override
    public void close() {
        this.isClosed = true;
    }
}
