package com.rosy.nano.transport.remoting;

import com.rosy.nano.transport.command.RemotingCommand;
import com.rosy.nano.transport.connection.Connection;
import io.netty.bootstrap.Bootstrap;

public interface RemotingClient extends RemotingService {

    Connection getOrCreateConnection(String addr);

    void initBootstrap(Bootstrap bootstrap);

    RemotingCommand invokeSync(String addr, RemotingCommand request, long timeoutNanos);

    void invokeAsync(String addr, RemotingCommand request, long timeoutNanos, RemotingCallback callback);

    void invokeOneway(String addr, RemotingCommand request, long timeoutNanos);

}
