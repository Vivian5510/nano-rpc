package com.rosy.nano.transport.remoting;

import com.rosy.nano.transport.command.RemotingCommand;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public interface RemotingCallback {

    void onComplete();

    default void onSuccess(RemotingCommand response) {

    }

    default void onFailure(Throwable t) {

    }

    default Executor executor() {
        return ForkJoinPool.commonPool();
    }
}
