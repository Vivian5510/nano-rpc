package com.rosy.nano.protocol.rpc.command.header;

import com.rosy.nano.protocol.rpc.serialization.BodySerializeType;

import java.util.Map;

public final class RpcRequestHeader extends RpcCommonHeader {

    public RpcRequestHeader() {
    }

    public RpcRequestHeader(BodySerializeType type, String bodyClassName) {
        super(type, bodyClassName);
    }

    @Override
    public void encodeTo(Map<String, String> extFields) {
        super.encodeTo(extFields);
    }

    @Override
    public void decodeFrom(Map<String, String> extFields) {
        super.decodeFrom(extFields);
    }
}
