package com.rosy.nano.protocol.rpc.remoting;

import com.google.common.base.Preconditions;
import com.rosy.nano.protocol.rpc.command.RpcRequest;
import com.rosy.nano.protocol.rpc.command.RpcResponse;
import com.rosy.nano.protocol.rpc.command.header.RpcCommonHeader;
import com.rosy.nano.protocol.rpc.command.header.RpcRequestHeader;
import com.rosy.nano.protocol.rpc.command.header.RpcResponseHeader;
import com.rosy.nano.protocol.rpc.serialization.BodySerializeType;
import com.rosy.nano.protocol.rpc.serialization.BodySerializer;
import com.rosy.nano.protocol.rpc.serialization.BodySerializerRegistry;
import com.rosy.nano.transport.command.RemotingCommand;
import com.rosy.nano.transport.remoting.BaseRemoting;
import com.rosy.nano.transport.serialization.HeaderSerializeType;

public class RpcRemoting extends BaseRemoting {
    public RemotingCommand toRequest(RpcRequest body, int code, HeaderSerializeType hType, BodySerializeType bType) {
        Preconditions.checkArgument(body != null, "body can't be null");
        Preconditions.checkArgument(hType != null, "header serialize type can't be null");
        Preconditions.checkArgument(bType != null, "body serialize type can't be null");

        RpcRequestHeader header = new RpcRequestHeader(bType, body.getClass().getName());
        RemotingCommand request = RemotingCommand.newRequest(code, header, hType);

        BodySerializer bSerializer = BodySerializerRegistry.get(bType);
        request.setBody(bSerializer.encode(body));

        return request;
    }

    public RemotingCommand toResponse(int code, RemotingCommand request, RpcResponse body, BodySerializeType bType, HeaderSerializeType hType) {
        Preconditions.checkArgument(request != null, "request can't be null");
        Preconditions.checkArgument(body != null, "body can't be null");
        Preconditions.checkArgument(hType != null, "header serialize type can't be null");
        Preconditions.checkArgument(bType != null, "body serialize type can't be null");

        RpcResponseHeader header = new RpcResponseHeader(bType, body.getClass().getName());
        RemotingCommand response = RemotingCommand.newResponse(code, request, header, hType);

        BodySerializer bSerializer = BodySerializerRegistry.get(bType);
        response.setBody(bSerializer.encode(body));
        return response;
    }

    @SuppressWarnings("unchecked")
    public <T extends RpcResponse> T decodeResponse(RemotingCommand command) {
        Preconditions.checkArgument(command.isResponse(), "command isn't response");
        return (T) decodeBody(command);
    }

    @SuppressWarnings("unchecked")
    public <T extends RpcRequest> T decodeRequest(RemotingCommand command) {
        Preconditions.checkArgument(!command.isResponse(), "command isn't request");
        return (T) decodeBody(command);
    }

    public Object decodeBody(RemotingCommand command) {
        if (command.getBody() == null || command.getBody().length == 0) return null;

        RpcCommonHeader header = command.isResponse() ? new RpcResponseHeader() : new RpcRequestHeader();
        header = command.decodeCustomHeader(header);
        BodySerializer serializer = BodySerializerRegistry.get(header.getBodySerializerType());
        return serializer.decode(command.getBody(), header.getBodyClassName());
    }
}
