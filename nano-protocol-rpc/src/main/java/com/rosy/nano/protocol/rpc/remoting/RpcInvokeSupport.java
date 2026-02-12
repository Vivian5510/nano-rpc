package com.rosy.nano.protocol.rpc.remoting;

import com.rosy.nano.protocol.rpc.command.RpcResponse;
import com.rosy.nano.protocol.rpc.command.RpcResponseCode;
import com.rosy.nano.protocol.rpc.command.header.RpcResponseHeader;
import com.rosy.nano.protocol.rpc.exception.RpcRemotingException;
import com.rosy.nano.transport.command.RemotingCommand;
import com.rosy.nano.transport.remoting.RemotingCallback;

import java.util.concurrent.Executor;

public final class RpcInvokeSupport {
    private final RpcRemoting remoting;

    RpcInvokeSupport(RpcRemoting remoting) {
        this.remoting = remoting;
    }

    RpcRemoting remoting() {
        return remoting;
    }

    <T extends RpcResponse> T decodeResponse(RemotingCommand response) {
        if (response.getCode() != RpcResponseCode.SUCCESS) {
            RpcResponseHeader header = response.decodeCustomHeader(new RpcResponseHeader());
            throw new RpcRemotingException(header.getErrorMessage());
        }
        return remoting.decodeResponse(response);
    }

    RemotingCallback wrapCallback(RpcCallback<? extends RpcResponse> cb) {
        return new RemotingCallback() {
            @Override
            public void onComplete() {
                cb.onComplete();
            }

            @Override
            public void onSuccess(RemotingCommand response) {
                cb.onSuccess(decodeResponse(response));
            }

            @Override
            public void onFailure(Throwable t) {
                cb.onFailure(t);
            }

            @Override
            public Executor executor() {
                return cb.executor();
            }
        };
    }
}
