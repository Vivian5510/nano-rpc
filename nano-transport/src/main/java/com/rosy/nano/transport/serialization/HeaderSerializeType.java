package com.rosy.nano.transport.serialization;

import com.google.common.base.Preconditions;
import lombok.Getter;

public enum HeaderSerializeType {
    JSON(0);

    private static final HeaderSerializeType[] LOOKUP = new HeaderSerializeType[256];

    static {
        for (HeaderSerializeType t : HeaderSerializeType.values()) {
            int i = t.code;
            Preconditions.checkState(LOOKUP[i] == null, "dup code in HeaderSerializeType: %s", i);
            LOOKUP[i] = t;
        }
    }

    @Getter
    private final int code;

    HeaderSerializeType(int code) {
        Preconditions.checkArgument(code >= 0 && code <= 255, "code out of range: %s", code);
        this.code = code;
    }

    public static HeaderSerializeType from(int code) {
        Preconditions.checkArgument(code >= 0 && code <= 255, "code out of range: %s", code);
        Preconditions.checkState(LOOKUP[code] != null, "unknown code in HeaderSerializeType: %s", code);
        return LOOKUP[code];
    }
}
