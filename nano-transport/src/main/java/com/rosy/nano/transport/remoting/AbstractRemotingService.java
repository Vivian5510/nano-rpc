package com.rosy.nano.transport.remoting;

import com.google.common.base.Preconditions;
import com.rosy.nano.transport.command.RemotingCommand;
import com.rosy.nano.transport.event.ChannelEventListener;
import com.rosy.nano.transport.event.ChannelEventListenerChain;
import com.rosy.nano.transport.event.NettyEventDispatcher;
import com.rosy.nano.transport.handler.*;
import com.rosy.nano.transport.lifecycle.LifeCycleSupport;
import com.rosy.nano.transport.processor.RequestProcessor;
import com.rosy.nano.transport.processor.RequestProcessorRegistry;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Setter;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public abstract class AbstractRemotingService implements RemotingService {
    
    private static final int CHANNEL_IDLE_MS = 90_000;

    private final LifeCycleSupport SUPPORT = new LifeCycleSupport();

    private final BaseRemoting remoting;
    private final RequestProcessorRegistry registry;
    private final RequestProcessor defaultProcessor;
    @Setter
    private Executor defaultProcessorExecutor;

    private final ChannelEventListenerChain eventListenerChain = new ChannelEventListenerChain();
    private final NettyEventDispatcher eventDispatcher = new NettyEventDispatcher(eventListenerChain);

    // sharable handlers
    private final NettyEncoder encoder = new NettyEncoder();
    private final BackPressureHandler backPressure = new BackPressureHandler();
    private final CommandProcessHandler process = new CommandProcessHandler(this);

    protected AbstractRemotingService(BaseRemoting remoting, RequestProcessorRegistry registry, RequestProcessor defaultProcessor) {
        this(remoting, registry, defaultProcessor, null);
    }

    protected AbstractRemotingService(BaseRemoting remoting, RequestProcessorRegistry registry, RequestProcessor defaultProcessor, Executor defaultProcessorExecutor) {
        this.remoting = Preconditions.checkNotNull(remoting, "BaseRemoting can't be null");
        this.registry = Preconditions.checkNotNull(registry, "RequestProcessorRegistry can't be null");
        this.defaultProcessor = Preconditions.checkNotNull(defaultProcessor, "DefaultRequestProcessor can't be null");
        this.defaultProcessorExecutor = defaultProcessorExecutor;
    }

    @Override
    public BaseRemoting remoting() {
        return remoting;
    }

    @Override
    public void registerProcessor(int code, RequestProcessor processor) {
        registry.register(code, processor);
    }

    @Override
    public RequestProcessor defaultProcessor() {
        return defaultProcessor;
    }

    @Override
    public Executor defaultProcessorExecutor() {
        return defaultProcessorExecutor;
    }

    @Override
    public void launch() {
        SUPPORT.launch();
        eventDispatcher.launch();
    }

    @Override
    public void shutdown() {
        SUPPORT.shutdown();
        eventDispatcher.shutdown();
    }

    @Override
    public void process(ChannelHandlerContext ctx, RemotingCommand command) {
        Preconditions.checkArgument(command != null, "command received is null");

        if (command.isResponse()) {
            processResponse(ctx, command);
        } else {
            processRequest(ctx, command);
        }
    }

    private void processRequest(ChannelHandlerContext ctx, RemotingCommand request) {
        RequestProcessor processor = registry.match(request.getCode());
        if (processor == null) processor = defaultProcessor();

        Preconditions.checkState(processor != null, "no processor found for code=%s", request.getCode());

        final RequestProcessor p = processor;
        Executor e = processor.executor() != null ? processor.executor() : defaultProcessorExecutor();

        Runnable task = () -> {
            try {
                RemotingCommand response = p.process(ctx, request);
                if (!request.isOneWay()) {
                    Preconditions.checkState(response != null, "response can't be null for two-way request");
                    Preconditions.checkState(response.isResponse(), "command type must be response here");
                    Preconditions.checkState(response.getOpaque() == request.getOpaque(), "opaque must be equal");
                    ctx.writeAndFlush(response);
                }
            } catch (Exception t) {
                ctx.fireExceptionCaught(t);
            }
        };

        if (e == null) task.run();
        else e.execute(task);
    }

    private void processResponse(ChannelHandlerContext ctx, RemotingCommand command) {
        remoting().onResponse(command);
    }

    @Override
    public RequestProcessorRegistry registry() {
        return registry;
    }

    @Override
    public final void addEventListener(ChannelEventListener listener) {
        eventListenerChain.addEventListener(listener);
    }

    protected final ChannelInitializer<SocketChannel> commonChannelInitializer() {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
                p.addLast("encoder", encoder);
                p.addLast("decoder", new NettyDecoder());
                p.addLast("idle", new IdleStateHandler(0, 0, CHANNEL_IDLE_MS, TimeUnit.MILLISECONDS));
                beforeCommonPipeline(p, ch); // 给子类扩展
                p.addLast("conn-manage", new NettyConnectManageHandler(eventDispatcher, isServerSide()));
                afterCommonPipeline(p, ch); // 给子类扩展
                p.addLast("back-pressure", backPressure);
                p.addLast("process", process);
            }
        };
    }

    protected void beforeCommonPipeline(ChannelPipeline p, SocketChannel ch) {
        // default no-op
    }

    protected void afterCommonPipeline(ChannelPipeline p, SocketChannel ch) {
        // default no-op
    }
}
