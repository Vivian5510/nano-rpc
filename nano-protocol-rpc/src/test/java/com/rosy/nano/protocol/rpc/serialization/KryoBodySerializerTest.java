package com.rosy.nano.protocol.rpc.serialization;

import com.rosy.nano.transport.exception.RemotingSerializationException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class KryoBodySerializerTest {
    private KryoBodySerializer serializer;

    @BeforeMethod
    public void setup() {
        serializer = new KryoBodySerializer();
    }

    @Test
    public void should_roundtrip_request_object() {
        UserRequest req = new UserRequest(1L, "rosy");

        byte[] bytes = serializer.encode(req);
        Object decoded = serializer.decode(bytes, UserRequest.class.getName());

        assertThat(decoded).isInstanceOf(UserRequest.class);
        assertThat(decoded).isEqualTo(req);
    }

    @Test
    public void should_throw_when_encode_null() {
        assertThatThrownBy(() -> serializer.encode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("body can't be null");
    }

    @Test
    public void should_throw_when_decode_with_wrong_class() {
        UserRequest req = new UserRequest(1L, "rosy");
        byte[] bytes = serializer.encode(req);
        assertThatThrownBy(() -> serializer.decode(bytes, OtherRequest.class.getName()))
                .isInstanceOf(RemotingSerializationException.class)
                .hasMessage("Exception occurred while KryoBodySerializer decoding");
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRequest {
        private long userId;
        private String name;
    }

    private static class OtherRequest {
        private int x;
    }
}
