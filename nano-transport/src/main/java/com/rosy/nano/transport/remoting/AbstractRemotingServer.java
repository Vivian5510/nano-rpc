package com.rosy.nano.transport.remoting;

import com.rosy.nano.transport.command.RemotingCommand;
import com.rosy.nano.transport.connection.Connection;
import com.rosy.nano.transport.exception.RemotingLifecycleException;
import com.rosy.nano.transport.processor.RequestProcessor;
import com.rosy.nano.transport.processor.RequestProcessorRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;

import java.util.concurrent.Executor;

public abstract class AbstractRemotingServer extends AbstractRemotingService implements RemotingServer {

    private final String ip;
    private final int port;
    private final ServerBootstrap bootstrap;
    private ServerChannel serverChannel;

    protected AbstractRemotingServer(String ip, int port, BaseRemoting remoting, RequestProcessorRegistry registry, RequestProcessor defaultProcessor) {
        this(ip, port, remoting, registry, defaultProcessor, null);
    }

    protected AbstractRemotingServer(String ip, int port, BaseRemoting remoting, RequestProcessorRegistry registry, RequestProcessor defaultProcessor, Executor defaultProcessorExecutor) {
        super(remoting, registry, defaultProcessor, defaultProcessorExecutor);
        this.ip = ip;
        this.port = port;
        this.bootstrap = new ServerBootstrap();
    }

    @Override
    public String ip() {
        return ip;
    }

    @Override
    public int port() {
        return port;
    }

    protected final ServerBootstrap bootstrap() {
        return bootstrap;
    }

    protected final ServerChannel serverChannel() {
        return serverChannel;
    }

    @Override
    public void launch() {
        super.launch();
        boolean ok = false;
        RemotingLifecycleException startEx = null;
        try {
            initBootstrap(bootstrap());
            ChannelFuture bindFuture = bootstrap().bind(ip(), port()).sync();
            if (bindFuture.isSuccess()) {
                serverChannel = (ServerChannel) bindFuture.channel();
                ok = true;
            } else {
                startEx = new RemotingLifecycleException("bind failed: " + ip() + ":" + port(), bindFuture.cause());
                throw startEx;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            startEx = new RemotingLifecycleException("Interrupted while starting server", e);
            throw startEx;
        } finally {
            if (!ok) {
                try {
                    shutdown();
                } catch (Throwable shutdownEx) {
                    if (startEx != null) startEx.addSuppressed(shutdownEx);
                    else throw shutdownEx;
                }
            }
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();

        if (serverChannel() == null) return;

        serverChannel.close().addListener(f -> {
            EventLoopGroup boss = bootstrap().config().group();
            EventLoopGroup worker = bootstrap().config().childGroup();

            if (boss != null) boss.shutdownGracefully();
            if (worker != null) worker.shutdownGracefully();
        });
    }

    @Override
    public void invokeOneway(Connection conn, RemotingCommand request, long timeoutNanos) {
        remoting().invokeOneway(conn, request, timeoutNanos);
    }

    @Override
    public void invokeAsync(Connection conn, RemotingCommand request, long timeoutNanos, RemotingCallback callback) {
        remoting().invokeAsync(conn, request, timeoutNanos, callback);
    }

    @Override
    public RemotingCommand invokeSync(Connection conn, RemotingCommand request, long timeoutNanos) {
        return remoting().invokeSync(conn, request, timeoutNanos);
    }
}
