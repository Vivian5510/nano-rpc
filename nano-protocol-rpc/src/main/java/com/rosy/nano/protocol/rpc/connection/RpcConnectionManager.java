package com.rosy.nano.protocol.rpc.connection;

import com.google.common.base.Preconditions;
import com.rosy.nano.transport.exception.RemotingConnectException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class RpcConnectionManager {

    private final ConcurrentMap<String, CountDownLatch> INFLIGHT = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RpcConnectionPool> POOLS = new ConcurrentHashMap<>();

    public RpcConnection getOrCreate(String addr, Supplier<RpcConnection> supplier) {
        RpcConnection existing = getActive(addr);
        if (existing != null) return existing;

        AtomicBoolean owner = new AtomicBoolean(false);
        CountDownLatch latch = INFLIGHT.computeIfAbsent(addr, k -> {
            owner.set(true);
            return new CountDownLatch(1);
        });

        if (owner.get()) {
            try {
                RpcConnection connection = Preconditions.checkNotNull(supplier.get(), "connection can't be null");
                POOLS.compute(addr, (a, p) -> {
                    RpcConnectionPool pool = p != null ? p : new RpcConnectionPool();
                    pool.add(connection);
                    return pool;
                });
            } catch (Exception e) {
                throw new RemotingConnectException("Error occurred when creating connection", e);
            } finally {
                latch.countDown();
                INFLIGHT.remove(addr, latch);
            }
        } else {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RemotingConnectException("connection fetch interrupted", e);
            }
        }

        RpcConnection active = getActive(addr);
        if (active == null) {
            throw new RemotingConnectException("connection not available: " + addr);
        }
        return active;
    }

    private RpcConnection getActive(String addr) {
        RpcConnectionPool pool = POOLS.get(addr);
        if (pool == null) return null;
        return pool.getActive();
    }

    public void remove(String addr, RpcConnection conn) {
        if (POOLS.containsKey(addr)) {
            POOLS.get(addr).remove(conn);
        }
    }
}
