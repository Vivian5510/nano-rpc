package com.rosy.nano.transport.handler;

import com.rosy.nano.transport.event.NettyEvent;
import com.rosy.nano.transport.event.NettyEventDispatcher;
import com.rosy.nano.transport.event.NettyEventType;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.net.SocketAddress;

public class NettyConnectManageHandler extends ChannelDuplexHandler {
    private final NettyEventDispatcher dispatcher;

    public NettyConnectManageHandler(NettyEventDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress,
                        ChannelPromise promise) throws Exception {
        super.connect(ctx, remoteAddress, localAddress, promise);
        dispatch(NettyEventType.CONNECT, ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        dispatch(NettyEventType.ACTIVE, ctx);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        closeChannel(ctx);
        super.disconnect(ctx, promise);
        dispatch(NettyEventType.CLOSE, ctx);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        closeChannel(ctx);
        super.close(ctx, promise);
        dispatch(NettyEventType.CLOSE, ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        closeChannel(ctx);
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent event) {
            if (event.state() == IdleState.ALL_IDLE) {
                closeChannel(ctx);
                dispatch(NettyEventType.IDLE, ctx);
            }
        }
        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        closeChannel(ctx);
        dispatch(NettyEventType.EXCEPTION, ctx);
    }

    private void closeChannel(ChannelHandlerContext ctx) {
        if (ctx.channel() != null) {
            ctx.channel().close();
        }
    }

    private void dispatch(NettyEventType type, ChannelHandlerContext ctx) {
        if (dispatcher == null) return;
        dispatcher.put(new NettyEvent(type, ctx.channel()));
    }
}
