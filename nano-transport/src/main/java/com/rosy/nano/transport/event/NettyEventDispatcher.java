package com.rosy.nano.transport.event;

import com.rosy.nano.transport.lifecycle.LifeCycle;
import com.rosy.nano.transport.lifecycle.LifeCycleSupport;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public final class NettyEventDispatcher implements Runnable, LifeCycle {
    private static final int DEFAULT_QUEUE_SIZE = 10000;
    private static final long DROP_LOG_INTERVAL_MILLIS = 1000L;

    private final LifeCycleSupport SUPPORT = new LifeCycleSupport();
    private final BlockingQueue<NettyEvent> queue;
    private final int queueCapacity;
    private final ChannelEventListener listener;
    private final Thread worker;
    private volatile boolean running = true;
    private final AtomicLong lastDropLogMillis = new AtomicLong(0L);

    public NettyEventDispatcher(ChannelEventListener listener) {
        this(listener, DEFAULT_QUEUE_SIZE);
    }

    public NettyEventDispatcher(ChannelEventListener listener, int queueSize) {
        this.listener = listener;
        this.queue = new LinkedBlockingQueue<>(queueSize);
        this.queueCapacity = queueSize;
        this.worker = new Thread(this, "netty-event-dispatcher");
        this.worker.setDaemon(true);
    }

    @Override
    public void launch() {
        SUPPORT.launch();
        worker.start();
    }

    @Override
    public void shutdown() {
        SUPPORT.shutdown();
        running = false;
        worker.interrupt();
    }

    public void put(NettyEvent event) {
        if (event == null || listener == null) return;
        if (queue.offer(event)) return;
        maybeLogDrop(event);
    }

    private void maybeLogDrop(NettyEvent event) {
        long now = System.currentTimeMillis();
        long prev = lastDropLogMillis.get();
        if (now - prev < DROP_LOG_INTERVAL_MILLIS) return;
        if (!lastDropLogMillis.compareAndSet(prev, now)) return;

        log.warn("Netty event queue full, drop event: capacity={}, size={}, type={}, channelId={}, remote={}",
            queueCapacity, queue.size(), event.type(), event.channel().id(), event.channel().remoteAddress());
    }

    @Override
    public void run() {
        while (running) {
            try {
                NettyEvent event = queue.poll(1, TimeUnit.SECONDS);
                if (event == null) continue;

                switch (event.type()) {
                    case CONNECT:
                        listener.onChannelConnect(event.channel());
                        break;
                    case CLOSE:
                        listener.onChannelClose(event.channel());
                        break;
                    case EXCEPTION:
                        listener.onChannelException(event.channel());
                        break;
                    case IDLE:
                        listener.onChannelIdle(event.channel());
                        break;
                    case ACTIVE:
                        listener.onChannelActive(event.channel());
                        break;
                    default:
                        break;
                }
            } catch (Throwable ignored) {

            }
        }
    }
}
