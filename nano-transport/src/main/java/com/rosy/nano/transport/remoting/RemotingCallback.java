package com.rosy.nano.transport.remoting;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public interface RemotingCallback {

    void onComplete();

    default void onSuccess() {

    }

    default void onFailure() {

    }

    default Executor executor() {
        return ForkJoinPool.commonPool();
    }
}
