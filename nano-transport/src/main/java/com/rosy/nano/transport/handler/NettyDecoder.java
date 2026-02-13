package com.rosy.nano.transport.handler;

import com.rosy.nano.transport.command.RemotingCommand;
import com.rosy.nano.transport.exception.RemotingSerializationException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public final class NettyDecoder extends LengthFieldBasedFrameDecoder {

    private static final int FRAME_MAX_LENGTH = 16 * 1024 * 1024;

    public NettyDecoder() {
        super(FRAME_MAX_LENGTH, 0, 4, 0, 4);
    }

    @Override
    public Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) return null;
        try {
            return RemotingCommand.decode(frame);
        } catch (Exception e) {
            throw new RemotingSerializationException("Failed to decode remoting frame", e);
        } finally {
            frame.release();
        }
    }
}
