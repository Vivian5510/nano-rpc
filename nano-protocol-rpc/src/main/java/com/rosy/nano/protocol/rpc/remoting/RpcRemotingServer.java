package com.rosy.nano.protocol.rpc.remoting;

import com.rosy.nano.protocol.rpc.command.RpcRequest;
import com.rosy.nano.protocol.rpc.command.RpcRequestCode;
import com.rosy.nano.protocol.rpc.command.RpcResponse;
import com.rosy.nano.protocol.rpc.connection.RpcConnection;
import com.rosy.nano.protocol.rpc.processor.RpcHeartbeatProcessor;
import com.rosy.nano.protocol.rpc.processor.RpcRequestProcessor;
import com.rosy.nano.protocol.rpc.processor.RpcRequestProcessorRegistry;
import com.rosy.nano.protocol.rpc.processor.rpc.UserProcessorRegistry;
import com.rosy.nano.protocol.rpc.serialization.BodySerializeType;
import com.rosy.nano.transport.command.RemotingCommand;
import com.rosy.nano.transport.handler.BackPressureHandler;
import com.rosy.nano.transport.handler.CommandProcessHandler;
import com.rosy.nano.transport.handler.NettyDecoder;
import com.rosy.nano.transport.handler.NettyEncoder;
import com.rosy.nano.transport.processor.RequestProcessor;
import com.rosy.nano.transport.remoting.AbstractRemotingServer;
import com.rosy.nano.transport.serialization.HeaderSerializeType;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.concurrent.Executor;

public final class RpcRemotingServer extends AbstractRemotingServer implements RpcServer {

    private final HeaderSerializeType hType;
    private final BodySerializeType bType;
    private final RpcInvokeSupport SUPPORT;

    public RpcRemotingServer(String ip, int port, RpcRemoting remoting, RequestProcessor defaultProcessor, HeaderSerializeType hType, BodySerializeType bType) {
        this(ip, port, remoting, defaultProcessor, null, hType, bType);
    }

    public RpcRemotingServer(String ip, int port, RpcRemoting remoting, RequestProcessor defaultProcessor, Executor defaultProcessorExecutor, HeaderSerializeType hType, BodySerializeType bType) {
        super(ip, port, remoting, new RpcRequestProcessorRegistry(), defaultProcessor, defaultProcessorExecutor);
        this.hType = hType;
        this.bType = bType;
        this.SUPPORT = new RpcInvokeSupport(remoting);
        registerProcessor(RpcRequestCode.RPC_REQUEST, new RpcRequestProcessor(SUPPORT.remoting(), new UserProcessorRegistry()));
        registerProcessor(RpcRequestCode.RPC_HEARTBEAT, new RpcHeartbeatProcessor());
    }

    @Override
    public void initBootstrap(ServerBootstrap bootstrap) {
        EventLoopGroup boss = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        EventLoopGroup worker = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

        NettyEncoder encoder = new NettyEncoder();
        BackPressureHandler backPressure = new BackPressureHandler();
        CommandProcessHandler process = new CommandProcessHandler(this);

        bootstrap.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast("encoder", encoder);
                        p.addLast("decoder", new NettyDecoder());
                        p.addLast("back-pressure", backPressure);
                        p.addLast("process", process);
                    }
                });
    }

    @Override
    public <T extends RpcResponse> T invokeSync(RpcConnection conn, RpcRequest request, long timeoutNanos) {
        RemotingCommand command = SUPPORT.remoting().toRequest(request, RpcRequestCode.RPC_REQUEST, hType, bType);
        return SUPPORT.decodeResponse(invokeSync(conn, command, timeoutNanos));
    }

    @Override
    public void invokeAsync(RpcConnection conn, RpcRequest request, long timeoutNanos, RpcCallback<? extends RpcResponse> callback) {
        RemotingCommand command = SUPPORT.remoting().toRequest(request, RpcRequestCode.RPC_REQUEST, hType, bType);
        invokeAsync(conn, command, timeoutNanos, SUPPORT.wrapCallback(callback));
    }

    @Override
    public void invokeOneway(RpcConnection conn, RpcRequest request, long timeoutNanos) {
        RemotingCommand command = SUPPORT.remoting().toRequest(request, RpcRequestCode.RPC_REQUEST, hType, bType);
        command.markOneway();
        invokeOneway(conn, command, timeoutNanos);
    }
}
