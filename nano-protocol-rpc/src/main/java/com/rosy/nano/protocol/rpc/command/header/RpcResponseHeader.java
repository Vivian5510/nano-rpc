package com.rosy.nano.protocol.rpc.command.header;

import com.rosy.nano.protocol.rpc.serialization.BodySerializeType;
import lombok.Getter;

import java.util.Map;

@Getter
public final class RpcResponseHeader extends RpcCommonHeader {

    private static final String K_RPC_RESPONSE_ERROR_MESSAGE = "rpc.response.error.message";

    private String errorMessage;

    public RpcResponseHeader() {
    }

    public RpcResponseHeader(BodySerializeType type, String bodyClassName) {
        super(type, bodyClassName);
    }

    public RpcResponseHeader(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    protected void encodeTo(Map<String, String> ext) {
        if (errorMessage != null) {
            ext.put(K_RPC_RESPONSE_ERROR_MESSAGE, errorMessage);
            return;
        }
        super.encodeTo(ext);
    }

    @Override
    protected void decodeFrom(Map<String, String> ext) {
        if (ext.containsKey(K_RPC_RESPONSE_ERROR_MESSAGE)) {
            errorMessage = ext.get(K_RPC_RESPONSE_ERROR_MESSAGE);
            return;
        }
        super.decodeFrom(ext);
    }
}
