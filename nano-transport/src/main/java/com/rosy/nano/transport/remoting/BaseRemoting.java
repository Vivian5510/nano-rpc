package com.rosy.nano.transport.remoting;

import com.rosy.nano.transport.command.RemotingCommand;
import com.rosy.nano.transport.connection.Connection;
import com.rosy.nano.transport.exception.RemotingInvokeInterruptedException;
import com.rosy.nano.transport.exception.RemotingInvokeTimeoutException;
import com.rosy.nano.transport.exception.RemotingSendRequestException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public abstract class BaseRemoting {

    final ConcurrentMap<Long, RemotingFuture> PENDING_REQUESTS = new ConcurrentHashMap<>(256);

    private static <T> CompletableFuture<T> failed(Throwable t) {
        CompletableFuture<T> f = new CompletableFuture<>();
        f.completeExceptionally(t);
        return f;
    }

    public void onResponse(RemotingCommand response) {
        RemotingFuture future = PENDING_REQUESTS.remove(response.getOpaque());
        if (future != null) future.complete(response);
    }

    public void scanPendingRequests() {
        long nowNanos = System.nanoTime();
        List<RemotingFuture> overtime = new ArrayList<>(16);

        for (RemotingFuture rf : PENDING_REQUESTS.values()) {
            if (rf.isTimeout(nowNanos) && PENDING_REQUESTS.remove(rf.opaque(), rf)) {
                overtime.add(rf);
            }
        }

        for (RemotingFuture rf : overtime) {
            rf.fail(new TimeoutException("request timeout: opaque=" + rf.opaque()));
        }
    }

    // TODO 加上类似 RocketMQ 的限流机制
    public RemotingCommand invokeSync(Connection conn, RemotingCommand request, long timeoutNanos) {
        try {
            return invoke0(conn, request, timeoutNanos).get(timeoutNanos, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RemotingInvokeInterruptedException(request.getOpaque(), conn.ch().remoteAddress().toString(), request.getCode(), "request interrupted", e);
        } catch (TimeoutException e) {
            throw new RemotingInvokeTimeoutException(request.getOpaque(), conn.ch().remoteAddress().toString(), request.getCode(), "request timeout", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new RemotingSendRequestException(request.getOpaque(), conn.ch().remoteAddress().toString(), request.getCode(), "request execution failed", cause);
        }
    }

    public void invokeAsync(Connection conn, RemotingCommand request, long timeoutNanos, RemotingCallback callback) {
        invoke0(conn, request, timeoutNanos).whenCompleteAsync((response, ex) -> {
            if (ex == null) callback.onSuccess(response);
            else callback.onFailure(ex);
            callback.onComplete();
        }, callback.executor() == null ? ForkJoinPool.commonPool() : callback.executor());
    }

    private CompletableFuture<RemotingCommand> invoke0(Connection conn, RemotingCommand request, long timeoutNanos) {
        long opaque = request.getOpaque();

        RemotingFuture future = new DefaultRemotingFuture(opaque, conn, timeoutNanos);
        if (PENDING_REQUESTS.putIfAbsent(opaque, future) != null) {
            future.fail(new IllegalStateException("duplicate opaque=" + opaque));
            return future.promise();
        }

        conn.ch().writeAndFlush(request).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {

            }
        });

        return future.promise();
    }

    public void invokeOneway(Connection conn, RemotingCommand request, long timeoutNanos) {
        conn.ch().writeAndFlush(request);
    }

}
