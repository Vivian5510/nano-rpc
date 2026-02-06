package com.rosy.nano.transport.remoting;

import com.rosy.nano.transport.lifecycle.LifeCycle;
import com.rosy.nano.transport.processor.RequestProcessor;

import java.util.concurrent.Executor;

public interface RemotingService extends LifeCycle {

    void registerProcessor(int code, RequestProcessor processor);

    void setDefaultProcessor(RequestProcessor processor);

    void setDefaultProcessorExecutor(Executor executor);
    
}
