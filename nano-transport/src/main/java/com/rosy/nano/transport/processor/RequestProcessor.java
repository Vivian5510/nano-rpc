package com.rosy.nano.transport.processor;

import com.rosy.nano.transport.command.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.Executor;

public interface RequestProcessor {

    RemotingCommand process(RemotingCommand request, ChannelHandlerContext ctx);

    void setExecutor(Executor executor);

}
