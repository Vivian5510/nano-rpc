package com.rosy.nano.transport.remoting;

import com.rosy.nano.transport.command.RemotingCommand;
import com.rosy.nano.transport.connection.Connection;
import io.netty.bootstrap.ServerBootstrap;

public interface RemotingServer extends RemotingService {

    String ip();

    int port();

    void initBootstrap(ServerBootstrap bootstrap);

    RemotingCommand invokeSync(Connection conn, RemotingCommand request, long timeoutNanos);

    void invokeAsync(Connection conn, RemotingCommand request, long timeoutNanos, RemotingCallback callback);

    void invokeOneway(Connection conn, RemotingCommand request, long timeoutNanos);
}
