package com.rosy.nano.protocol.rpc.processor;

import com.rosy.nano.transport.command.RemotingCommand;
import com.rosy.nano.transport.processor.RequestProcessor;
import io.netty.channel.ChannelHandlerContext;

public class RpcRequestProcessor implements RequestProcessor {
    @Override
    public RemotingCommand process(ChannelHandlerContext ctx, RemotingCommand request) {
        return null;
    }
}
