package com.rosy.nano.protocol.rpc.connection;

import com.rosy.nano.transport.connection.Connection;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public class RpcConnection implements Connection {

    public static final AttributeKey<RpcConnection> RPC_CONN = AttributeKey.valueOf("nano.rpc.connection");

    private final String addr;
    private final Channel ch;

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

    public void close() {
        if (ch != null) {
            ch.close();
        }
    }

    public static RpcConnection getOrCreate(Channel ch) {
        RpcConnection existed = ch.attr(RPC_CONN).get();
        if(existed != null) return existed;

        String remoteAddr = ch.remoteAddress() == null ? "unknown" : ch.remoteAddress().toString();
        RpcConnection created = new RpcConnection(remoteAddr, ch);
        if(ch.attr(RPC_CONN).compareAndSet(null, created)) return created;
        // 并发创建
        return ch.attr(RPC_CONN).get();
    }
}
