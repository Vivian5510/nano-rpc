package com.rosy.nano.protocol.rpc.serialization;

/**
 * Body 序列化器。
 */
public interface BodySerializer {

    BodySerializeType type();

    byte[] encode(Object bodyObject);

    Object decode(byte[] bodyBytes, String bodyClass);
}
