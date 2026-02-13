package com.rosy.nano.protocol.rpc.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.auto.service.AutoService;
import com.google.common.base.Preconditions;
import com.rosy.nano.transport.exception.RemotingSerializationException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@AutoService(BodySerializer.class)
public class KryoBodySerializer implements BodySerializer {

    private static final int INIT_BUFFER_SIZE = 256;

    private final ConcurrentMap<String, Class<?>> classCache = new ConcurrentHashMap<>();

    private final ThreadLocal<Kryo> KRYO = ThreadLocal.withInitial(() -> {
        Kryo k = new Kryo();
        k.setRegistrationRequired(false);
        k.setReferences(true);
        return k;
    });

    private final ThreadLocal<Output> OUTPUT = ThreadLocal.withInitial(() -> {
        return new Output(INIT_BUFFER_SIZE, -1);
    });

    private final ThreadLocal<Input> INPUT = ThreadLocal.withInitial(() -> {
        return new Input(INIT_BUFFER_SIZE);
    });

    @Override
    public BodySerializeType type() {
        return BodySerializeType.KRYO;
    }

    @Override
    public byte[] encode(Object body) {
        Preconditions.checkArgument(body != null, "body can't be null");

        try {
            Kryo kryo = KRYO.get();
            Output output = OUTPUT.get();

            output.reset();

            kryo.writeClassAndObject(output, body);
            output.flush();
            return output.toBytes();
        } catch (Exception e) {
            throw new RemotingSerializationException("Exception occurred while KryoBodySerializer encoding", e);
        }
    }

    @Override
    public Object decode(byte[] bodyBytes, String bodyClass) {
        try {
            Kryo kryo = KRYO.get();
            Input input = INPUT.get();

            input.setBuffer(bodyBytes);

            Object decoded = kryo.readClassAndObject(input);
            Class<?> expected = classCache.computeIfAbsent(bodyClass, this::loadClass);
            Preconditions.checkArgument(
                    expected.isInstance(decoded),
                    "decoded type mismatch, expected=%s, actual=%s",
                    bodyClass, decoded == null ? "null" : decoded.getClass().getName());

            return decoded;
        } catch (Exception e) {
            throw new RemotingSerializationException("Exception occurred while KryoBodySerializer decoding", e);
        }
    }

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("class not found: " + className, e);
        }
    }
}
