package com.rosy.nano.transport.lifecycle;

import java.util.concurrent.atomic.AtomicBoolean;

public class LifeCycleSupport implements LifeCycle {

    private final AtomicBoolean isStarted = new AtomicBoolean(false);

    @Override
    public void launch() {
        if (isStarted.compareAndSet(false, true)) return;
        throw new IllegalArgumentException("this component has already launched");
    }

    @Override
    public void shutdown() {
        if (isStarted.compareAndSet(true, false)) return;
        throw new IllegalArgumentException("this component has already shutdown");
    }

    public boolean isLaunched() {
        return isStarted.get();
    }
}
