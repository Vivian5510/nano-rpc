package com.rosy.nano.protocol.rpc.command.header;

import com.rosy.nano.transport.command.CustomCommandHeader;
import lombok.Getter;

import java.util.Map;

@Getter
public final class HeartbeatRequestHeader extends CustomCommandHeader {
    private static final String K_CLIENT_TIME = "hb.client.time";

    private long clientTime;

    public HeartbeatRequestHeader() {
        this(System.currentTimeMillis());
    }

    public HeartbeatRequestHeader(long clientTime) {
        this.clientTime = clientTime;
    }

    @Override
    protected void encodeTo(Map<String, String> ext) {
        ext.put(K_CLIENT_TIME, String.valueOf(clientTime));
    }

    @Override
    protected void decodeFrom(Map<String, String> ext) {
        String v = ext.get(K_CLIENT_TIME);
        if (v != null) {
            clientTime = Long.parseLong(v);
        }
    }
}
