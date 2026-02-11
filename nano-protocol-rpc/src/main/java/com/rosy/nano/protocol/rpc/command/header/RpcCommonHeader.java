package com.rosy.nano.protocol.rpc.command.header;

import com.google.common.base.Preconditions;
import com.rosy.nano.protocol.rpc.serialization.BodySerializeType;
import com.rosy.nano.transport.command.CustomCommandHeader;
import lombok.Getter;

import java.util.Map;

@Getter
public abstract class RpcCommonHeader extends CustomCommandHeader {
    protected static final String K_BODY_SERIALIZER = "rpc.body.serializer.type";
    protected static final String K_BODY_CLASS = "rpc.body.class.name";

    protected int bodySerializerType;
    protected String bodyClassName;

    protected RpcCommonHeader() {
    }

    protected RpcCommonHeader(BodySerializeType type, String name) {
        bodySerializerType = Preconditions.checkNotNull(type, "type null").code();
        bodyClassName = Preconditions.checkNotNull(name, "name null");
    }

    @Override
    protected void encodeTo(Map<String, String> ext) {
        ext.put(K_BODY_SERIALIZER, String.valueOf(bodySerializerType));
        ext.put(K_BODY_CLASS, bodyClassName);
    }

    @Override
    protected void decodeFrom(Map<String, String> ext) {
        bodySerializerType = Integer.parseInt(requireExt(ext, K_BODY_SERIALIZER));
        bodyClassName = requireExt(ext, K_BODY_CLASS);
    }
}