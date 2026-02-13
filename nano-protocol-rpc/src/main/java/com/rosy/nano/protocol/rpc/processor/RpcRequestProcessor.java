package com.rosy.nano.protocol.rpc.processor;

import com.rosy.nano.protocol.rpc.command.RpcRequest;
import com.rosy.nano.protocol.rpc.command.RpcResponse;
import com.rosy.nano.protocol.rpc.command.RpcResponseCode;
import com.rosy.nano.protocol.rpc.command.header.RpcRequestHeader;
import com.rosy.nano.protocol.rpc.command.header.RpcResponseHeader;
import com.rosy.nano.protocol.rpc.processor.rpc.UserProcessor;
import com.rosy.nano.protocol.rpc.processor.rpc.UserProcessorRegistry;
import com.rosy.nano.protocol.rpc.remoting.RpcRemoting;
import com.rosy.nano.protocol.rpc.serialization.BodySerializeType;
import com.rosy.nano.transport.command.RemotingCommand;
import com.rosy.nano.transport.processor.RequestProcessor;
import io.netty.channel.ChannelHandlerContext;

public class RpcRequestProcessor implements RequestProcessor {

    private final RpcRemoting remoting;
    private final UserProcessorRegistry registry;

    public RpcRequestProcessor(RpcRemoting remoting, UserProcessorRegistry registry) {
        this.remoting = remoting;
        this.registry = registry;
    }

    @Override
    public RemotingCommand process(ChannelHandlerContext ctx, RemotingCommand request) {
        RpcRequestHeader header = request.decodeCustomHeader(new RpcRequestHeader());
        RpcRequest rpcRequest = remoting.decodeRequest(request);
        String interest = header.getBodyClassName();
        UserProcessor processor = registry.match(interest);

        if(processor == null) return RemotingCommand.newResponse(RpcResponseCode.NO_PROCESSOR, request, new RpcResponseHeader("no processor for interest=" + interest), request.getHeaderSerializeType());

        try {
            RpcResponse rpcResponse = processor.process(ctx, rpcRequest);
            return remoting.toResponse(
                    RpcResponseCode.SUCCESS,
                    request,
                    rpcResponse,
                    BodySerializeType.from(header.getBodySerializerType()),
                    request.getHeaderSerializeType()
            );
        } catch (Exception e) {
            return RemotingCommand.newResponse(RpcResponseCode.INTERNAL_ERROR, request, new RpcResponseHeader("unexpected error happen when process rpc-request. info=" + e.getMessage()), request.getHeaderSerializeType() );
        }
    }
}
