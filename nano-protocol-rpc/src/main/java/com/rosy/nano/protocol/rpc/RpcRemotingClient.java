package com.rosy.nano.protocol.rpc;

import com.rosy.nano.transport.connection.Connection;
import com.rosy.nano.transport.processor.RequestProcessor;
import com.rosy.nano.transport.processor.RequestProcessorRegistry;
import com.rosy.nano.transport.remoting.AbstractRemotingClient;
import com.rosy.nano.transport.remoting.BaseRemoting;
import io.netty.bootstrap.Bootstrap;

import java.util.concurrent.Executor;

public class RpcRemotingClient extends AbstractRemotingClient {
    protected RpcRemotingClient(BaseRemoting remoting, RequestProcessorRegistry registry, RequestProcessor defaultProcessor) {
        this(remoting, registry, defaultProcessor, null);
    }

    protected RpcRemotingClient(BaseRemoting remoting, RequestProcessorRegistry registry, RequestProcessor defaultProcessor, Executor defaultProcessorExecutor) {
        super(remoting, registry, defaultProcessor, defaultProcessorExecutor);
    }

    @Override
    public Connection getOrCreateConnection(String addr) {
        return null;
    }

    @Override
    public void initBootstrap(Bootstrap bootstrap) {

    }
}
