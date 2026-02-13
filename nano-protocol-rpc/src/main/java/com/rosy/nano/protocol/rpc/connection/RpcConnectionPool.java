package com.rosy.nano.protocol.rpc.connection;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class RpcConnectionPool {
    private final CopyOnWriteArrayList<RpcConnection> connections = new CopyOnWriteArrayList<>();
    private final AtomicInteger idx = new AtomicInteger(0);
    private volatile long lastAccessMillis = System.currentTimeMillis();

    private void markAccess() {
        lastAccessMillis = System.currentTimeMillis();
    }

    public long lastAccessMillis() {
        return lastAccessMillis;
    }

    public int size() {
        return connections.size();
    }

    public boolean isEmpty() {
        return connections.isEmpty();
    }

    public List<RpcConnection> getAll() {
        return new ArrayList<>(connections);
    }

    public RpcConnectionPool add(RpcConnection conn) {
        Preconditions.checkArgument(conn != null, "add null to ConnectionPool");
        markAccess();
        connections.addIfAbsent(conn);
        return this;
    }

    public RpcConnection getActive() {
        markAccess();
        if (connections.isEmpty()) return null;
        List<RpcConnection> snapshot = new ArrayList<>(connections);
        int size = snapshot.size();

        for (int i = 0; i < size; i++) {
            int index = Math.floorMod(idx.getAndIncrement(), size);
            RpcConnection conn = snapshot.get(index);
            if (conn != null) {
                if (conn.isActive()) return conn;
                else {
                    connections.remove(conn);
                    conn.close();
                }
            }
        }

        return null;
    }

    public void remove(RpcConnection conn) {
        Preconditions.checkArgument(conn != null, "remove null in ConnectionPool");
        markAccess();
        connections.remove(conn);
    }

    public void scan() {
        for (RpcConnection conn : connections) {
            if (conn == null || !conn.isActive()) {
                connections.remove(conn);
                if (conn != null) conn.close();
            }
        }
    }

    public void closeAll() {
        for (RpcConnection conn : connections) {
            if (conn != null) conn.close();
        }
        connections.clear();
    }
}
