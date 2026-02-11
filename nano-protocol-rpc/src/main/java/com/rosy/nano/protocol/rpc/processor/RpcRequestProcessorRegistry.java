package com.rosy.nano.protocol.rpc.processor;

import com.rosy.nano.transport.processor.RequestProcessor;
import com.rosy.nano.transport.processor.RequestProcessorRegistry;

public class RpcRequestProcessorRegistry implements RequestProcessorRegistry {
    @Override
    public RequestProcessor match(int code) {
        return null;
    }

    @Override
    public void register(int code, RequestProcessor processor) {

    }
}
