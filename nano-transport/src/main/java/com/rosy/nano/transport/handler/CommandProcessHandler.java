package com.rosy.nano.transport.handler;

import com.google.common.base.Preconditions;
import com.rosy.nano.transport.command.RemotingCommand;
import com.rosy.nano.transport.processor.RequestProcessor;
import com.rosy.nano.transport.processor.RequestProcessorRegistry;
import com.rosy.nano.transport.remoting.RemotingService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@ChannelHandler.Sharable
public class CommandProcessHandler extends SimpleChannelInboundHandler<RemotingCommand> {
    private final RemotingService remotingService;

    public CommandProcessHandler(RemotingService remotingService) {
        this.remotingService = Preconditions.checkNotNull(remotingService, "RemotingService can't be null");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand command) throws Exception {
        remotingService.process(ctx, command);
    }
}
