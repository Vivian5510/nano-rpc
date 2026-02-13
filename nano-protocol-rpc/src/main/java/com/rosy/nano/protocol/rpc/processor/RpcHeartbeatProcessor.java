package com.rosy.nano.protocol.rpc.processor;

import com.rosy.nano.protocol.rpc.command.RpcResponseCode;
import com.rosy.nano.protocol.rpc.command.header.HeartbeatResponseHeader;
import com.rosy.nano.transport.command.RemotingCommand;
import com.rosy.nano.transport.processor.RequestProcessor;
import io.netty.channel.ChannelHandlerContext;
import com.rosy.nano.transport.serialization.HeaderSerializeType;

public class RpcHeartbeatProcessor implements RequestProcessor {
    @Override
    public RemotingCommand process(ChannelHandlerContext ctx, RemotingCommand request) {
        if (request.isOneWay()) {
            throw new IllegalArgumentException("RPC_HEARTBEAT doesn't accept oneway request");
        }

        HeaderSerializeType hType = request.getHeaderSerializeType();
        HeartbeatResponseHeader header = new HeartbeatResponseHeader(System.currentTimeMillis());
        return RemotingCommand.newResponse(RpcResponseCode.SUCCESS, request, header, hType);
    }
}
