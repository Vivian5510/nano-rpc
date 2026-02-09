package com.rosy.nano.transport.serialization;

import com.google.common.base.Preconditions;

import java.util.EnumMap;
import java.util.ServiceLoader;

public final class HeaderSerializerRegistry {

    private static final EnumMap<HeaderSerializeType, HeaderSerializer> MAP = new EnumMap<>(HeaderSerializeType.class);

    private HeaderSerializerRegistry() {}

    static {
        ServiceLoader.load(HeaderSerializer.class, HeaderSerializerRegistry.class.getClassLoader()).forEach(s -> {
            HeaderSerializeType t = Preconditions.checkNotNull(s.type(), "type null");
            HeaderSerializer old = MAP.put(t, s);
            Preconditions.checkState(old == null, "dup HeaderSerializer for %s", t);
        });

        for (HeaderSerializeType t : HeaderSerializeType.values()) {
            Preconditions.checkState(MAP.containsKey(t), "missing HeaderSerializer for %s", t);
        }
    }

    public static HeaderSerializer get(int code) {
        return get(HeaderSerializeType.from(code));
    }

    public static HeaderSerializer get(HeaderSerializeType type) {
        Preconditions.checkArgument(MAP.containsKey(type), "no HeaderSerializer for %s", type);
        return MAP.get(type);
    }
}
