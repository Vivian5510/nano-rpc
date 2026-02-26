package com.rosy.nano.protocol.rpc.handler;

import com.rosy.nano.protocol.rpc.connection.RpcConnection;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class RpcConnectionInitHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        RpcConnection.getOrCreate(ctx.channel());
        super.channelActive(ctx);
    }
}
