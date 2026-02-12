package com.rosy.nano.protocol.rpc.remoting;

import com.rosy.nano.protocol.rpc.processor.rpc.UserProcessor;
import com.rosy.nano.protocol.rpc.processor.rpc.UserProcessorRegistry;

public interface RpcRemotingService {

    UserProcessorRegistry uRegistry();

    default void registerUserProcessor(UserProcessor processor) {
        uRegistry().register(processor);
    }
}
