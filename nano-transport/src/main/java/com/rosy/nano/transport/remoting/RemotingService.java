package com.rosy.nano.transport.remoting;

import com.rosy.nano.transport.command.RemotingCommand;
import com.rosy.nano.transport.lifecycle.LifeCycle;
import com.rosy.nano.transport.processor.RequestProcessor;
import com.rosy.nano.transport.processor.RequestProcessorRegistry;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.Executor;

public interface RemotingService extends LifeCycle {

    BaseRemoting remoting();

    RequestProcessorRegistry registry();

    RequestProcessor defaultProcessor();

    Executor defaultProcessorExecutor();

    default void registerProcessor(int code, RequestProcessor processor) {
        registry().register(code, processor);
    }

    void process(ChannelHandlerContext ctx, RemotingCommand command);
}
