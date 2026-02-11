package com.rosy.nano.protocol.rpc.connection;

import com.google.common.base.Preconditions;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class RpcConnectionPool {
    private final CopyOnWriteArrayList<RpcConnection> connections = new CopyOnWriteArrayList<>();
    private final AtomicInteger idx = new AtomicInteger(0);

    public RpcConnectionPool add(RpcConnection conn) {
        Preconditions.checkArgument(conn != null, "add null to ConnectionPool");
        connections.addIfAbsent(conn);
        return this;
    }

    public RpcConnection getActive() {
        if (connections.isEmpty()) return null;
        int size = connections.size();

        for (int i = 0; i < size; i++) {
            int index = Math.floorMod(idx.getAndIncrement(), size);
            RpcConnection conn = connections.get(index);
            if (conn != null) {
                if (conn.isActive()) return conn;
                else connections.remove(conn);
            }
        }

        return null;
    }

    public void remove(RpcConnection conn) {
        Preconditions.checkArgument(conn != null, "remove null in ConnectionPool");
        connections.remove(conn);
    }
}
