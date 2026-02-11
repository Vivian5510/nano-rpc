package com.rosy.nano.protocol.rpc.command.header;

import com.rosy.nano.protocol.rpc.serialization.BodySerializeType;

public final class RpcResponseHeader extends RpcCommonHeader {

    public RpcResponseHeader() {
    }

    public RpcResponseHeader(BodySerializeType type, String bodyClassName) {
        super(type, bodyClassName);
    }
}
