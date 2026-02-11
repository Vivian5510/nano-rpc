package com.rosy.nano.protocol.rpc.connection;

import com.rosy.nano.transport.connection.Connection;
import io.netty.channel.Channel;

public class RpcConnection implements Connection {

    private final String addr;
    private Channel ch;

    public RpcConnection(String addr, Channel ch) {
        this.addr = addr;
        this.ch = ch;
    }

    public String remoteAddr() {
        return addr;
    }

    @Override
    public Channel ch() {
        return ch;
    }

    public boolean isActive() {
        return ch != null && ch.isActive();
    }
}
