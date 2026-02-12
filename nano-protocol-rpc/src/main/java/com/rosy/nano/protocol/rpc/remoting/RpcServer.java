package com.rosy.nano.protocol.rpc.remoting;

import com.rosy.nano.protocol.rpc.command.RpcRequest;
import com.rosy.nano.protocol.rpc.command.RpcResponse;
import com.rosy.nano.protocol.rpc.connection.RpcConnection;
import com.rosy.nano.transport.remoting.RemotingServer;

public interface RpcServer extends RemotingServer, RpcRemotingService {
    <T extends RpcResponse> T invokeSync(RpcConnection conn, RpcRequest request, long timeoutNanos);

    void invokeAsync(RpcConnection conn, RpcRequest request, long timeoutNanos, RpcCallback<? extends RpcResponse> callback);

    void invokeOneway(RpcConnection conn, RpcRequest request, long timeoutNanos);
}
