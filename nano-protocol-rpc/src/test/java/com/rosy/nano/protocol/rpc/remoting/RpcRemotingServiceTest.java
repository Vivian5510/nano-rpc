package com.rosy.nano.protocol.rpc.remoting;

import com.rosy.nano.protocol.rpc.command.RpcRequest;
import com.rosy.nano.protocol.rpc.command.RpcResponse;
import com.rosy.nano.protocol.rpc.processor.rpc.UserProcessor;
import com.rosy.nano.protocol.rpc.serialization.BodySerializeType;
import com.rosy.nano.transport.connection.Connection;
import com.rosy.nano.transport.processor.RequestProcessor;
import com.rosy.nano.transport.serialization.HeaderSerializeType;
import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class RpcRemotingServiceTest {
    private RpcRemotingServer server;
    private RpcRemotingClient client;
    private int port;

    @BeforeClass
    public void setUp() throws Exception {
        port = findFreePort();

        RequestProcessor defaultProcessor = (ctx, req) -> {
            throw new IllegalStateException("no processor for request code=" + req.getCode());
        };

        server = new RpcRemotingServer("127.0.0.1", port, new RpcRemoting(),
                defaultProcessor, HeaderSerializeType.JSON, BodySerializeType.KRYO
        );
        server.registerUserProcessor(new AddProcessor());
        server.launch();

        client = new RpcRemotingClient(new RpcRemoting(), defaultProcessor, HeaderSerializeType.JSON, BodySerializeType.KRYO);
        client.launch();
    }

    @AfterClass
    public void tearDown() {
        server.shutdown();
        client.shutdown();
    }

    @Test
    public void should_connect_to_server() {
        Connection conn = client.getOrCreateConnection("127.0.0.1:" + port);
        assertThat(conn.ch().isActive()).isEqualTo(true);
    }

    @Test
    public void should_add_numbers() {
        AddResponse response = client.invokeSync(
                "127.0.0.1:" + port,
                new AddRequest(1, 2),
                TimeUnit.SECONDS.toNanos(10)
        );
        assertThat(response.getSum()).isEqualTo(3);
    }

    private static int findFreePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddRequest implements RpcRequest {
        private int left;
        private int right;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddResponse implements RpcResponse {
        private int sum;
    }

    public static class AddProcessor implements UserProcessor {
        @Override
        public String interest() {
            return AddRequest.class.getName();
        }

        @Override
        public RpcResponse process(ChannelHandlerContext ctx, RpcRequest request) {
            AddRequest r = (AddRequest) request;
            return new AddResponse(r.getLeft() + r.getRight());
        }
    }
}
