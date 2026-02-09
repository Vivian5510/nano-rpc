package com.rosy.nano.protocol.rpc;

import com.rosy.nano.transport.processor.RequestProcessor;
import com.rosy.nano.transport.processor.RequestProcessorRegistry;
import com.rosy.nano.transport.remoting.AbstractRemotingServer;
import com.rosy.nano.transport.remoting.BaseRemoting;
import io.netty.bootstrap.ServerBootstrap;

import java.util.concurrent.Executor;

public class RpcRemotingServer extends AbstractRemotingServer {

    protected RpcRemotingServer(String ip, int port, BaseRemoting remoting, RequestProcessorRegistry registry, RequestProcessor defaultProcessor) {
        super(ip, port, remoting, registry, defaultProcessor, null);
    }

    protected RpcRemotingServer(String ip, int port, BaseRemoting remoting, RequestProcessorRegistry registry, RequestProcessor defaultProcessor, Executor defaultProcessorExecutor) {
        super(ip, port, remoting, registry, defaultProcessor, defaultProcessorExecutor);
    }

    @Override
    public void initBootstrap(ServerBootstrap bootstrap) {

    }
}
