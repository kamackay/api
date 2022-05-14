package com.keithmackay.api.utils;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.util.BlockingArrayQueue;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class Channel<T> implements Closeable {

    private boolean isClosed;

    private BlockingQueue<T> feed;

    private List<Runnable> listeners;

    public Channel() {
        this.isClosed = false;
        this.listeners = new ArrayList<>();
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
                log.info("Interruption in Channel", e);
            }
        }
    }

    public void push(final T t) {
        this.feed.add(t);
    }

    @Override
    public void close() throws IOException {
        this.isClosed = true;
        this.listeners.clear();
    }
}
