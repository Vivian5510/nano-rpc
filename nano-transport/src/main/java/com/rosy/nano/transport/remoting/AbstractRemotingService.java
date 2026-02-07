package com.rosy.nano.transport.remoting;

import com.google.common.base.Preconditions;
import com.rosy.nano.transport.command.RemotingCommand;
import com.rosy.nano.transport.processor.RequestProcessor;
import com.rosy.nano.transport.processor.RequestProcessorRegistry;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.Executor;

public abstract class AbstractRemotingService implements RemotingService {

    BaseRemoting remoting;
    RequestProcessorRegistry registry;
    RequestProcessor defaultProcessor;
    Executor defaultProcessorExecutor;

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
    public void process(ChannelHandlerContext ctx, RemotingCommand command) {
        Preconditions.checkArgument(command != null, "command received is null");

        if(command.isResponse()) {
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
                if(!request.isOneWay()) {
                    Preconditions.checkState(response != null, "response can't be null for two-way request");
                    Preconditions.checkState(response.isResponse(), "command type must be response here");
                    Preconditions.checkState(response.getOpaque() == request.getOpaque(), "opaque must be equal");
                    ctx.writeAndFlush(response);
                }
            } catch (Exception t) {
                ctx.fireExceptionCaught(t);
            }
        };

        if(e == null) task.run();
        else e.execute(task);
    }

    private void processResponse(ChannelHandlerContext ctx, RemotingCommand command) {
        remoting().onResponse(command);
    }

    @Override
    public RequestProcessorRegistry registry() {
        return registry;
    }
}
