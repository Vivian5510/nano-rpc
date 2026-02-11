package com.rosy.nano.protocol.rpc.remoting;

import com.google.common.base.Preconditions;
import com.google.common.net.HostAndPort;
import com.rosy.nano.protocol.rpc.command.RpcRequest;
import com.rosy.nano.protocol.rpc.command.RpcRequestCode;
import com.rosy.nano.protocol.rpc.command.RpcResponse;
import com.rosy.nano.protocol.rpc.connection.RpcConnection;
import com.rosy.nano.protocol.rpc.connection.RpcConnectionManager;
import com.rosy.nano.protocol.rpc.processor.RpcRequestProcessorRegistry;
import com.rosy.nano.protocol.rpc.serialization.BodySerializeType;
import com.rosy.nano.transport.command.RemotingCommand;
import com.rosy.nano.transport.connection.Connection;
import com.rosy.nano.transport.exception.RemotingConnectException;
import com.rosy.nano.transport.handler.BackPressureHandler;
import com.rosy.nano.transport.handler.CommandProcessHandler;
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

import java.util.concurrent.Executor;

public final class RpcRemotingClient extends AbstractRemotingClient implements RpcClient {
    private final RpcConnectionManager connectionManager = new RpcConnectionManager();
    private final HeaderSerializeType hType;
    private final BodySerializeType bType;
    private final RpcInvokeSupport SUPPORT;

    public RpcRemotingClient(RpcRemoting remoting, RpcRequestProcessorRegistry registry, RequestProcessor defaultProcessor, HeaderSerializeType hType, BodySerializeType bType) {
        this(remoting, registry, defaultProcessor, null, hType, bType);
    }

    public RpcRemotingClient(RpcRemoting remoting, RpcRequestProcessorRegistry registry, RequestProcessor defaultProcessor, Executor defaultProcessorExecutor, HeaderSerializeType hType, BodySerializeType bType) {
        super(remoting, registry, defaultProcessor, defaultProcessorExecutor);
        this.hType = hType;
        this.bType = bType;
        this.SUPPORT = new RpcInvokeSupport(remoting);
    }

    @Override
    public void initBootstrap(Bootstrap bootstrap) {
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        NettyEncoder encoder = new NettyEncoder();
        BackPressureHandler backPressure = new BackPressureHandler();
        CommandProcessHandler process = new CommandProcessHandler(this);

        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast("encoder", encoder);
                        p.addLast("decoder", new NettyDecoder());
                        p.addLast("back-pressure", backPressure);
                        p.addLast("process", process);
                    }
                });
    }

    @Override
    public Connection getOrCreateConnection(String addr) {
        return connectionManager.getOrCreate(addr, () -> doConnect(addr));
    }

    public RpcConnection doConnect(String addr) {
        HostAndPort hp = HostAndPort.fromString(addr).requireBracketsForIPv6();
        Preconditions.checkArgument(hp.hasPort(), "missing port in addr=%s", addr);

        ChannelFuture f = bootstrap().connect(hp.getHost(), hp.getPort()).syncUninterruptibly();

        if (!f.isSuccess() || !f.channel().isActive()) throw new RemotingConnectException("do connect fail");
        RpcConnection conn = new RpcConnection(addr, f.channel());
        f.channel().closeFuture().addListener(cf -> {
            connectionManager.remove(addr, conn);
        });

        return conn;
    }


    @Override
    public <T extends RpcResponse> T invokeSync(String addr, RpcRequest request, long timeoutNanos) {
        RemotingCommand command = SUPPORT.remoting().toRequest(request, RpcRequestCode.RPC_REQUEST, hType, bType);
        return SUPPORT.remoting().decodeResponse(invokeSync(addr, command, timeoutNanos));
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
}
