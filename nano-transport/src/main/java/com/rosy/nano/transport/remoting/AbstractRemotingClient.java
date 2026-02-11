package com.rosy.nano.transport.remoting;

import com.rosy.nano.transport.command.RemotingCommand;
import com.rosy.nano.transport.processor.RequestProcessor;
import com.rosy.nano.transport.processor.RequestProcessorRegistry;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;

import java.util.concurrent.Executor;

public abstract class AbstractRemotingClient extends AbstractRemotingService implements RemotingClient {

    private final Bootstrap bootstrap;

    protected AbstractRemotingClient(BaseRemoting remoting, RequestProcessorRegistry registry, RequestProcessor defaultProcessor) {
        this(remoting, registry, defaultProcessor, null);
    }

    protected AbstractRemotingClient(BaseRemoting remoting, RequestProcessorRegistry registry, RequestProcessor defaultProcessor, Executor defaultProcessorExecutor) {
        super(remoting, registry, defaultProcessor, defaultProcessorExecutor);
        bootstrap = new Bootstrap();
    }

    protected Bootstrap bootstrap() {
        return bootstrap;
    }

    @Override
    public void launch() {
        super.launch();
        initBootstrap(bootstrap());
    }

    @Override
    public void shutdown() {
        super.shutdown();
        EventLoopGroup group = bootstrap().config().group();
        if (group != null) group.shutdownGracefully();
    }

    @Override
    public RemotingCommand invokeSync(String addr, RemotingCommand request, long timeoutNanos) {
        return remoting().invokeSync(getOrCreateConnection(addr), request, timeoutNanos);
    }

    @Override
    public void invokeAsync(String addr, RemotingCommand request, long timeoutNanos, RemotingCallback callback) {
        remoting().invokeAsync(getOrCreateConnection(addr), request, timeoutNanos, callback);
    }

    @Override
    public void invokeOneway(String addr, RemotingCommand request, long timeoutNanos) {
        remoting().invokeOneway(getOrCreateConnection(addr), request, timeoutNanos);
    }
}
