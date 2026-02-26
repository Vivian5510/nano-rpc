package com.rosy.nano.protocol.rpc.remoting;

import com.rosy.nano.protocol.rpc.serialization.BodySerializeType;
import com.rosy.nano.transport.connection.Connection;
import com.rosy.nano.transport.event.ChannelEventListener;
import com.rosy.nano.transport.processor.RequestProcessor;
import com.rosy.nano.transport.serialization.HeaderSerializeType;
import io.netty.channel.Channel;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class RpcChannelEventListenerTest {
    private RpcRemotingServer server;
    private RpcRemotingClient client;
    private int port;

    private CountingListener serverListenerA;
    private CountingListener serverListenerB;
    private CountingListener clientListenerA;
    private CountingListener clientListenerB;

    @BeforeClass
    public void setUp() throws Exception {
        port = findFreePort();
        RequestProcessor defaultProcessor = (ctx, req) -> {
            throw new IllegalStateException("no processor for request code=" + req.getCode());
        };

        server = new RpcRemotingServer(
            "127.0.0.1",
            port,
            new RpcRemoting(),
            defaultProcessor,
            HeaderSerializeType.JSON,
            BodySerializeType.KRYO
        );
        serverListenerA = new CountingListener();
        serverListenerB = new CountingListener();
        server.addEventListener(serverListenerA);
        server.addEventListener(serverListenerB);
        server.launch();

        client = new RpcRemotingClient(
            new RpcRemoting(),
            defaultProcessor,
            HeaderSerializeType.JSON,
            BodySerializeType.KRYO
        );
        clientListenerA = new CountingListener();
        clientListenerB = new CountingListener();
        client.addEventListener(clientListenerA);
        client.addEventListener(clientListenerB);
        client.launch();
    }

    @AfterClass
    public void tearDown() {
        if (client != null) {
            client.shutdown();
        }
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void should_dispatch_active_and_close_events_on_client_and_server() throws Exception {
        Connection conn = client.getOrCreateConnection("127.0.0.1:" + port);
        assertThat(conn.ch().isActive()).isEqualTo(true);

        assertThat(clientListenerA.awaitActive(3, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(clientListenerB.awaitActive(3, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(serverListenerA.awaitActive(3, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(serverListenerB.awaitActive(3, TimeUnit.SECONDS)).isEqualTo(true);

        conn.ch().close().syncUninterruptibly();

        assertThat(clientListenerA.awaitClose(3, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(clientListenerB.awaitClose(3, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(serverListenerA.awaitClose(3, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(serverListenerB.awaitClose(3, TimeUnit.SECONDS)).isEqualTo(true);
    }

    private static int findFreePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    private static final class CountingListener implements ChannelEventListener {
        private final CountDownLatch activeLatch = new CountDownLatch(1);
        private final CountDownLatch closeLatch = new CountDownLatch(1);

        @Override
        public void onChannelActive(Channel channel) {
            activeLatch.countDown();
        }

        @Override
        public void onChannelClose(Channel channel) {
            closeLatch.countDown();
        }

        boolean awaitActive(long timeout, TimeUnit unit) throws InterruptedException {
            return activeLatch.await(timeout, unit);
        }

        boolean awaitClose(long timeout, TimeUnit unit) throws InterruptedException {
            return closeLatch.await(timeout, unit);
        }
    }
}
