package com.rosy.nano.protocol.rpc.serialization;

import com.google.common.base.Preconditions;

import java.util.EnumMap;
import java.util.ServiceLoader;

public final class BodySerializerRegistry {

    private static final EnumMap<BodySerializeType, BodySerializer> MAP = new EnumMap<>(BodySerializeType.class);

    static {
        ServiceLoader.load(BodySerializer.class, BodySerializerRegistry.class.getClassLoader()).forEach(s -> {
            BodySerializeType t = Preconditions.checkNotNull(s.type(), "BodySerializeType can't be null");
            BodySerializer old = MAP.put(t, s);
            Preconditions.checkState(old == null, "Duplicate BodySerializer for %s", t);
        });

        for (BodySerializeType t : BodySerializeType.values()) {
            Preconditions.checkState(MAP.containsKey(t), "Missing BodySerializer for %s", t);
        }
    }

    private BodySerializerRegistry() {
    }

    public static BodySerializer get(int code) {
        return get(BodySerializeType.from(code));
    }

    public static BodySerializer get(BodySerializeType type) {
        Preconditions.checkArgument(type != null, "BodySerializeType can't be null");
        BodySerializer serializer = MAP.get(type);
        Preconditions.checkState(serializer != null, "No BodySerializer for %s", type);
        return serializer;
    }
}
