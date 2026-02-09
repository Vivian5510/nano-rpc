package com.rosy.nano.protocol.rpc.serialization;

import java.util.EnumMap;
import java.util.ServiceLoader;

public final class BodySerializerRegistry {

    private static final EnumMap<BodySerializeType, BodySerializer> MAP = new EnumMap<>(BodySerializeType.class);

    static {
        ServiceLoader.load(BodySerializer.class, BodySerializerRegistry.class.getClassLoader()).forEach(s -> {
            BodySerializeType t = s.type();
            if (t == null) {
                throw new IllegalArgumentException("BodySerializeType can't be null");
            }
            BodySerializer old = MAP.put(t, s);
            if (old != null) {
                throw new IllegalStateException("Duplicate BodySerializer for " + t);
            }
        });

        for (BodySerializeType t : BodySerializeType.values()) {
            if (!MAP.containsKey(t)) {
                throw new IllegalStateException("Missing BodySerializer for " + t);
            }
        }
    }

    private BodySerializerRegistry() {
    }

    public static BodySerializer get(int code) {
        return get(BodySerializeType.from(code));
    }

    public static BodySerializer get(BodySerializeType type) {
        if (type == null) {
            throw new IllegalArgumentException("BodySerializeType can't be null");
        }
        BodySerializer serializer = MAP.get(type);
        if (serializer == null) {
            throw new IllegalStateException("No BodySerializer for " + type);
        }
        return serializer;
    }
}
