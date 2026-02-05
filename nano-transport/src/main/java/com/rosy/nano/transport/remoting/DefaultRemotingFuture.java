package com.rosy.nano.transport.remoting;

import com.rosy.nano.transport.command.RemotingCommand;
import com.rosy.nano.transport.connection.Connection;

import java.util.concurrent.CompletableFuture;

public class DefaultRemotingFuture implements RemotingFuture {

    final long opaque;
    final CompletableFuture<RemotingCommand> promise;
    final Connection connection;
    final long deadlineNanos;

    public DefaultRemotingFuture(long opaque, Connection connection, long timeoutNanos) {
        this(opaque, new CompletableFuture<>(), connection, timeoutNanos);
    }

    public DefaultRemotingFuture(long opaque, CompletableFuture<RemotingCommand> promise, Connection connection, long timeoutNanos) {
        this.opaque = opaque;
        this.promise = promise;
        this.connection = connection;
        this.deadlineNanos = System.nanoTime() + timeoutNanos;
    }

    @Override
    public long opaque() {
        return opaque;
    }

    @Override
    public Connection connection() {
        return connection;
    }

    @Override
    public long deadlineNanos() {
        return deadlineNanos;
    }

    @Override
    public CompletableFuture<RemotingCommand> promise() {
        return promise;
    }
}
