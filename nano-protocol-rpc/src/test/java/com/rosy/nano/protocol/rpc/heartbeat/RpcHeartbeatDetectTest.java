package com.rosy.nano.protocol.rpc.heartbeat;

import com.rosy.nano.protocol.rpc.remoting.RpcRemoting;
import com.rosy.nano.protocol.rpc.remoting.RpcRemotingClient;
import com.rosy.nano.protocol.rpc.serialization.BodySerializeType;
import com.rosy.nano.transport.connection.Connection;
import com.rosy.nano.transport.processor.RequestProcessor;
import com.rosy.nano.transport.serialization.HeaderSerializeType;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ReferenceCountUtil;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class RpcHeartbeatDetectTest {
    private EventLoopGroup boss;
    private EventLoopGroup worker;
    private Channel blackholeServer;

    private RpcRemotingClient client;
    private int port;

    @BeforeClass
    public void setUp() throws Exception {
        port = findFreePort();
        startBlackholeServer();

        RequestProcessor defaultProcessor = (ctx, req) -> {
            throw new IllegalStateException("no processor for request code=" + req.getCode());
        };
        client = new RpcRemotingClient(new RpcRemoting(), defaultProcessor, HeaderSerializeType.JSON, BodySerializeType.KRYO);
        client.launch();
    }

    @AfterClass
    public void tearDown() {
        if (client != null) {
            client.shutdown();
        }
        if (blackholeServer != null) {
            blackholeServer.close().syncUninterruptibly();
        }
        if (boss != null) {
            boss.shutdownGracefully().syncUninterruptibly();
        }
        if (worker != null) {
            worker.shutdownGracefully().syncUninterruptibly();
        }
    }

    @Test
    public void should_close_connection_when_heartbeat_timeout() throws Exception {
        Connection conn = client.getOrCreateConnection("127.0.0.1:" + port);
        assertThat(conn.ch().isActive()).isEqualTo(true);

        // First heartbeat is sent on channelActive; wait past timeout then scan to fail pending requests.
        Thread.sleep(HeartbeatService.HEARTBEAT_TIMEOUT_MS + 200);
        client.remoting().scanPendingRequests();
        assertThat(conn.ch().closeFuture().await(3, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(conn.ch().isActive()).isEqualTo(false);
    }

    private void startBlackholeServer() {
        boss = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        worker = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast("blackhole", new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                ReferenceCountUtil.release(msg);
                            }
                        });
                    }
                });

        blackholeServer = bootstrap.bind("127.0.0.1", port).syncUninterruptibly().channel();
    }

    private static int findFreePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}
