package com.rosy.nano.protocol.rpc.remoting;

import com.rosy.nano.protocol.rpc.command.RpcResponse;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public interface RpcCallback<T extends RpcResponse> {
    default void onComplete() {
    }

    void onSuccess(T response);

    void onFailure(Throwable t);

    default Executor executor() {
        return ForkJoinPool.commonPool();
    }
}
