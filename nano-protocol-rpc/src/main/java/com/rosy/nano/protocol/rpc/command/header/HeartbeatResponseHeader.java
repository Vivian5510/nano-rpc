package com.rosy.nano.protocol.rpc.command.header;

import com.rosy.nano.transport.command.CustomCommandHeader;
import lombok.Getter;

import java.util.Map;

@Getter
public final class HeartbeatResponseHeader extends CustomCommandHeader {
    private static final String K_SERVER_TIME = "hb.server.time";

    private long serverTime;

    public HeartbeatResponseHeader() {
        this(System.currentTimeMillis());
    }

    public HeartbeatResponseHeader(long serverTime) {
        this.serverTime = serverTime;
    }

    @Override
    protected void encodeTo(Map<String, String> ext) {
        ext.put(K_SERVER_TIME, String.valueOf(serverTime));
    }

    @Override
    protected void decodeFrom(Map<String, String> ext) {
        String v = ext.get(K_SERVER_TIME);
        if (v != null) {
            serverTime = Long.parseLong(v);
        }
    }
}
