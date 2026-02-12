package com.rosy.nano.protocol.rpc.command.header;

import com.google.common.base.Preconditions;
import com.rosy.nano.protocol.rpc.serialization.BodySerializeType;

public final class RpcRequestHeader extends RpcCommonHeader {

    public RpcRequestHeader() {
    }

    public RpcRequestHeader(BodySerializeType type, String name) {
        bodySerializerType = Preconditions.checkNotNull(type, "type null").code();
        bodyClassName = Preconditions.checkNotNull(name, "name null");
    }
}
