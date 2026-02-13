package com.rosy.nano.protocol.rpc.connection;

import com.google.common.base.Preconditions;
import com.rosy.nano.transport.lifecycle.LifeCycle;
import com.rosy.nano.transport.lifecycle.LifeCycleSupport;
import com.rosy.nano.transport.exception.RemotingConnectException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class RpcConnectionManager implements LifeCycle {

    private static final long DEFAULT_POOL_IDLE_MILLIS = TimeUnit.MINUTES.toMillis(5);
    private static final long DEFAULT_SCAN_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(30);

    private final LifeCycleSupport SUPPORT = new LifeCycleSupport();
    private final ConcurrentMap<String, CountDownLatch> INFLIGHT = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RpcConnectionPool> POOLS = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Supplier<RpcConnection>> SUPPLIERS = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicBoolean> HEALING = new ConcurrentHashMap<>();

    private final ScheduledExecutorService monitor;
    private final ExecutorService asyncCreator;

    private volatile int poolSize = 1;
    private final long poolIdleMillis;

    public RpcConnectionManager() {
        this(1, DEFAULT_POOL_IDLE_MILLIS);
    }

    public RpcConnectionManager(int poolSize) {
        this(poolSize, DEFAULT_POOL_IDLE_MILLIS);
    }

    public RpcConnectionManager(int poolSize, long poolIdleMillis) {
        this.poolIdleMillis = Math.max(0L, poolIdleMillis);
        this.monitor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rpc-conn-monitor");
            t.setDaemon(true);
            return t;
        });
        this.asyncCreator = Executors.newFixedThreadPool(1, r -> {
            Thread t = new Thread(r, "rpc-conn-warmup");
            t.setDaemon(true);
            return t;
        });
        setPoolSize(poolSize);
    }

    public void setPoolSize(int poolSize) {
        Preconditions.checkArgument(poolSize >= 1, "poolSize must be >= 1");
        this.poolSize = poolSize;
    }

    @Override
    public void launch() {
        SUPPORT.launch();
        monitor.scheduleAtFixedRate(this::scan, DEFAULT_SCAN_INTERVAL_MILLIS, DEFAULT_SCAN_INTERVAL_MILLIS,
            TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        SUPPORT.shutdown();
        monitor.shutdown();
        asyncCreator.shutdown();
        for (RpcConnectionPool pool : POOLS.values()) {
            if (pool != null) pool.closeAll();
        }
        POOLS.clear();
        INFLIGHT.clear();
        SUPPLIERS.clear();
        HEALING.clear();
    }

    public RpcConnection getOrCreate(String addr, Supplier<RpcConnection> supplier) {
        Preconditions.checkArgument(addr != null && !addr.isEmpty(), "addr can't be null or empty");
        Preconditions.checkArgument(supplier != null, "supplier can't be null");
        Preconditions.checkState(SUPPORT.isLaunched(), "RpcConnectionManager not launched");
        SUPPLIERS.putIfAbsent(addr, supplier);

        RpcConnection existing = getActive(addr);
        if (existing != null) {
            ensurePoolSize(addr);
            return existing;
        }

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
        ensurePoolSize(addr);
        return active;
    }

    private RpcConnection getActive(String addr) {
        RpcConnectionPool pool = POOLS.get(addr);
        if (pool == null) return null;
        return pool.getActive();
    }

    public void remove(String addr, RpcConnection conn) {
        RpcConnectionPool pool = POOLS.get(addr);
        if (pool != null && conn != null) pool.remove(conn);
    }

    private void scan() {
        if (!SUPPORT.isLaunched()) return;

        long now = System.currentTimeMillis();
        for (Map.Entry<String, RpcConnectionPool> entry : POOLS.entrySet()) {
            String addr = entry.getKey();
            RpcConnectionPool pool = entry.getValue();
            if (pool == null) continue;

            pool.scan();

            if (pool.isEmpty()) {
                AtomicBoolean healing = HEALING.get(addr);
                if (healing != null && healing.get()) {
                    continue;
                }
                if ((now - pool.lastAccessMillis()) > poolIdleMillis) {
                    POOLS.remove(addr, pool);
                    SUPPLIERS.remove(addr);
                    INFLIGHT.remove(addr);
                    HEALING.remove(addr);
                }
                continue;
            }

            if ((now - pool.lastAccessMillis()) <= poolIdleMillis) {
                ensurePoolSize(addr);
            }
        }
    }

    private void ensurePoolSize(String addr) {
        int target = poolSize;
        if (target <= 1) return;

        RpcConnectionPool pool = POOLS.get(addr);
        if (pool == null || pool.size() >= target) return;

        Supplier<RpcConnection> supplier = SUPPLIERS.get(addr);
        if (supplier == null) return;

        AtomicBoolean healing = HEALING.computeIfAbsent(addr, k -> new AtomicBoolean(false));
        if (!healing.compareAndSet(false, true)) return;

        asyncCreator.execute(() -> {
            try {
                while (pool.size() < target) {
                    RpcConnection conn = Preconditions.checkNotNull(supplier.get(), "connection can't be null");
                    pool.add(conn);
                }
            } catch (Exception ignored) {
                // best effort warm-up; next call/scan will retry
            } finally {
                healing.set(false);
            }
        });
    }
}
