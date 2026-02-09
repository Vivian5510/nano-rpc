package com.rosy.nano.transport.remoting;

import com.rosy.nano.transport.command.RemotingCommand;
import com.rosy.nano.transport.connection.Connection;

import java.util.concurrent.CompletableFuture;

public interface RemotingFuture {

    long opaque();

    Connection connection();

    long deadlineNanos();

    CompletableFuture<RemotingCommand> promise();

    default void complete(RemotingCommand response) {
        promise().complete(response);
    }

    default void fail(Throwable t) {
        promise().completeExceptionally(t);
    }

    default boolean isTimeout(long nowNanos) {
        return nowNanos > deadlineNanos();
    }
}
