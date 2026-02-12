package com.rosy.nano.protocol.rpc.remoting;

import com.rosy.nano.protocol.rpc.command.RpcRequest;
import com.rosy.nano.protocol.rpc.command.RpcResponse;
import com.rosy.nano.transport.remoting.RemotingClient;

public interface RpcClient extends RemotingClient, RpcRemotingService {
    <T extends RpcResponse> T invokeSync(String addr, RpcRequest request, long timeoutNanos);
    void invokeAsync(String addr, RpcRequest request, long timeoutNanos, RpcCallback<? extends RpcResponse> callback);
    void invokeOneway(String addr, RpcRequest request, long timeoutNanos);
}