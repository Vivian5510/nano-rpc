package com.rosy.nano.transport.remoting;

import com.rosy.nano.transport.command.RemotingCommand;
import com.rosy.nano.transport.connection.Connection;

public interface RemotingClient extends RemotingService {

    Connection getOrCreateConnection(String addr);

    RemotingCommand invokeSync(String addr, RemotingCommand request, long timeoutNanos);

    void invokeAsync(String addr, RemotingCommand request, long timeoutNanos, RemotingCallback callback);

    void invokeOneway(String addr, RemotingCommand request, long timeoutNanos);
    
}
