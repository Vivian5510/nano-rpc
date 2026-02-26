package com.rosy.nano.protocol.rpc.remoting;

import com.google.common.base.Preconditions;
import com.google.common.net.HostAndPort;
import com.rosy.nano.protocol.rpc.command.RpcRequest;
import com.rosy.nano.protocol.rpc.command.RpcRequestCode;
import com.rosy.nano.protocol.rpc.command.RpcResponse;
import com.rosy.nano.protocol.rpc.connection.RpcConnection;
import com.rosy.nano.protocol.rpc.connection.RpcConnectionManager;
import com.rosy.nano.protocol.rpc.heartbeat.HeartbeatService;
import com.rosy.nano.protocol.rpc.event.ClientChannelEventListener;
import com.rosy.nano.protocol.rpc.processor.RpcHeartbeatProcessor;
import com.rosy.nano.protocol.rpc.processor.RpcRequestProcessor;
import com.rosy.nano.protocol.rpc.processor.RpcRequestProcessorRegistry;
import com.rosy.nano.protocol.rpc.processor.rpc.UserProcessorRegistry;
import com.rosy.nano.protocol.rpc.serialization.BodySerializeType;
import com.rosy.nano.transport.command.RemotingCommand;
import com.rosy.nano.transport.connection.Connection;
import com.rosy.nano.transport.event.NettyEventDispatcher;
import com.rosy.nano.transport.exception.RemotingConnectException;
import com.rosy.nano.transport.handler.BackPressureHandler;
import com.rosy.nano.transport.handler.CommandProcessHandler;
import com.rosy.nano.transport.handler.NettyConnectManageHandler;
import com.rosy.nano.transport.handler.NettyDecoder;
import com.rosy.nano.transport.handler.NettyEncoder;
import com.rosy.nano.transport.processor.RequestProcessor;
import com.rosy.nano.transport.remoting.AbstractRemotingClient;
import com.rosy.nano.transport.serialization.HeaderSerializeType;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public final class RpcRemotingClient extends AbstractRemotingClient implements RpcClient {
    private final RpcConnectionManager connectionManager = new RpcConnectionManager();
    private final HeaderSerializeType hType;
    private final BodySerializeType bType;
    private final RpcInvokeSupport SUPPORT;
    private final UserProcessorRegistry uRegistry;
    private final HeartbeatService heartbeatService;

    public RpcRemotingClient(RpcRemoting remoting, RequestProcessor defaultProcessor, HeaderSerializeType hType, BodySerializeType bType) {
        this(remoting, defaultProcessor, null, hType, bType);
    }

    public RpcRemotingClient(RpcRemoting remoting, RequestProcessor defaultProcessor, Executor defaultProcessorExecutor, HeaderSerializeType hType, BodySerializeType bType) {
        super(remoting, new RpcRequestProcessorRegistry(), defaultProcessor, defaultProcessorExecutor);
        this.hType = hType;
        this.bType = bType;
        this.SUPPORT = new RpcInvokeSupport(remoting);
        this.uRegistry = new UserProcessorRegistry();
        registerProcessor(RpcRequestCode.RPC_REQUEST, new RpcRequestProcessor(SUPPORT.remoting(), uRegistry));
        registerProcessor(RpcRequestCode.RPC_HEARTBEAT, new RpcHeartbeatProcessor());

        this.heartbeatService = new HeartbeatService(remoting(), hType, remoting()::scanPendingRequests);
        addEventListener(new ClientChannelEventListener(heartbeatService));
    }

    @Override
    public void launch() {
        super.launch();
        connectionManager.launch();
        heartbeatService.launch();
    }

    @Override
    public void shutdown() {
        heartbeatService.shutdown();
        connectionManager.shutdown();
        super.shutdown();
    }

    @Override
    public void initBootstrap(Bootstrap bootstrap) {
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(commonChannelInitializer());
    }

    @Override
    public Connection getOrCreateConnection(String addr) {
        return connectionManager.getOrCreate(addr, () -> doConnect(addr));
    }

    public void setConnectionPoolSize(int poolSize) {
        connectionManager.setPoolSize(poolSize);
    }

    public RpcConnection doConnect(String addr) {
        HostAndPort hp = HostAndPort.fromString(addr).requireBracketsForIPv6();
        Preconditions.checkArgument(hp.hasPort(), "missing port in addr=%s", addr);

        ChannelFuture f = bootstrap().connect(hp.getHost(), hp.getPort()).syncUninterruptibly();

        if (!f.isSuccess() || !f.channel().isActive()) throw new RemotingConnectException("do connect fail");

        RpcConnection conn = RpcConnection.getOrCreate(f.channel());
        f.channel().closeFuture().addListener(cf -> {
            connectionManager.remove(addr, conn);
        });

        return conn;
    }


    @Override
    public <T extends RpcResponse> T invokeSync(String addr, RpcRequest request, long timeoutNanos) {
        RemotingCommand command = SUPPORT.remoting().toRequest(request, RpcRequestCode.RPC_REQUEST, hType, bType);
        return SUPPORT.decodeResponse(invokeSync(addr, command, timeoutNanos));
    }

    @Override
    public void invokeAsync(String addr, RpcRequest request, long timeoutNanos, RpcCallback<? extends RpcResponse> callback) {
        RemotingCommand command = SUPPORT.remoting().toRequest(request, RpcRequestCode.RPC_REQUEST, hType, bType);
        invokeAsync(addr, command, timeoutNanos, SUPPORT.wrapCallback(callback));
    }

    @Override
    public void invokeOneway(String addr, RpcRequest request, long timeoutNanos) {
        RemotingCommand command = SUPPORT.remoting().toRequest(request, RpcRequestCode.RPC_REQUEST, hType, bType);
        command.markOneway();
        invokeOneway(addr, command, timeoutNanos);
    }

    @Override
    public UserProcessorRegistry uRegistry() {
        return uRegistry;
    }
}
