package com.rosy.nano.protocol.rpc.serialization;

import com.google.common.base.Preconditions;

/**
 * Body 序列化类型编码（1 byte）。
 */
public enum BodySerializeType {

    KRYO(0);

    private static final BodySerializeType[] LOOKUP = new BodySerializeType[256];

    static {
        for (BodySerializeType t : values()) {
            int code = t.code;
            Preconditions.checkState(LOOKUP[code] == null, "Duplicate body serializer code: %s", code);
            LOOKUP[code] = t;
        }
    }

    private final int code;

    BodySerializeType(int code) {
        Preconditions.checkArgument(code >= 0 && code <= 255, "Serializer code out of range: %s", code);
        this.code = code;
    }

    public static BodySerializeType from(int code) {
        Preconditions.checkArgument(code >= 0 && code <= 255, "Serializer code out of range: %s", code);
        BodySerializeType type = LOOKUP[code];
        Preconditions.checkState(type != null, "Unknown body serializer code: %s", code);
        return type;
    }

    public int code() {
        return code;
    }
}
