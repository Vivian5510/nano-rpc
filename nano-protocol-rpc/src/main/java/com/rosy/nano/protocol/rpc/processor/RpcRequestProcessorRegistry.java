package com.rosy.nano.protocol.rpc.processor;

import com.google.common.base.Preconditions;
import com.rosy.nano.transport.processor.RequestProcessor;
import com.rosy.nano.transport.processor.RequestProcessorRegistry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RpcRequestProcessorRegistry implements RequestProcessorRegistry {

    private final ConcurrentMap<Integer, RequestProcessor> PROCESSORS;

    public RpcRequestProcessorRegistry() {
        PROCESSORS = new ConcurrentHashMap<>();
    }

    @Override
    public RequestProcessor match(int code) {
        return PROCESSORS.get(code);
    }

    @Override
    public void register(int code, RequestProcessor processor) {
        Preconditions.checkArgument(code >= 0, "code must be >= 0");
        Preconditions.checkArgument(processor != null, "processor can't be null");

        RequestProcessor old = PROCESSORS.putIfAbsent(code, processor);
        Preconditions.checkState(old == null, "duplicate processor registration for code=%s", code);
    }
}
