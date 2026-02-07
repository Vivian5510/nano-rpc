package com.rosy.nano.transport.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@ChannelHandler.Sharable
public class BackPressureHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        Channel ch = ctx.channel();
        if (ch.isWritable()) {
            if (!ch.config().isAutoRead()) {
                ch.config().setAutoRead(true);
            }
        } else {
            if (ch.config().isAutoRead()) {
                ch.config().setAutoRead(false);
            }
        }
        ctx.fireChannelWritabilityChanged();
    }
}
