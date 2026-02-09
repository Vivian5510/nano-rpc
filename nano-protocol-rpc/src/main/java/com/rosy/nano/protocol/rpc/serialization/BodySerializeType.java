package com.rosy.nano.protocol.rpc.serialization;

/**
 * Body 序列化类型编码（1 byte）。
 */
public enum BodySerializeType {

    PROTOBUF(0);

    private static final BodySerializeType[] LOOKUP = new BodySerializeType[256];

    static {
        for (BodySerializeType t : values()) {
            int code = t.code;
            if (LOOKUP[code] != null) {
                throw new IllegalStateException("Duplicate body serializer code: " + code);
            }
            LOOKUP[code] = t;
        }
    }

    private final int code;

    BodySerializeType(int code) {
        if (code < 0 || code > 255) {
            throw new IllegalArgumentException("Serializer code out of range: " + code);
        }
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static BodySerializeType from(int code) {
        if (code < 0 || code > 255) {
            throw new IllegalArgumentException("Serializer code out of range: " + code);
        }
        BodySerializeType type = LOOKUP[code];
        if (type == null) {
            throw new IllegalStateException("Unknown body serializer code: " + code);
        }
        return type;
    }
}
