package com.rosy.nano.protocol.rpc.processor.rpc;

import com.rosy.nano.protocol.rpc.command.RpcRequest;
import com.rosy.nano.protocol.rpc.command.RpcResponse;
import io.netty.channel.ChannelHandlerContext;

public interface UserProcessor {

    String interest();

     RpcResponse process(ChannelHandlerContext ctx, RpcRequest request);
}
