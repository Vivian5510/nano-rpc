package com.rosy.nano.protocol.rpc.connection;

import io.netty.channel.ChannelId;
import io.netty.channel.embedded.EmbeddedChannel;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class RpcConnectionManagerTest {
    private RpcConnectionManager manager;

    @BeforeClass
    public void setUp() {
        manager = new RpcConnectionManager();
        manager.setPoolSize(1);
        manager.launch();
    }

    @AfterClass
    public void tearDown() {
        manager.shutdown();
    }

    @Test
    public void should_create_connection_only_once_when_concurrent_get_or_create() throws Exception {
        String addr = "127.0.0.1:9999";
        AtomicInteger supplierCalls = new AtomicInteger(0);
        Supplier<RpcConnection> supplier = () -> {
            supplierCalls.incrementAndGet();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return new RpcConnection(addr, new EmbeddedChannel());
        };

        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch ready = new CountDownLatch(threads);
        List<Future<RpcConnection>> futures = new ArrayList<>(threads);

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                return manager.getOrCreate(addr, supplier);
            }));
        }

        ready.await();
        start.countDown();

        Set<ChannelId> ids = new HashSet<>();
        for (Future<RpcConnection> future : futures) {
            RpcConnection conn = future.get(5, TimeUnit.SECONDS);
            ids.add(conn.ch().id());
        }

        executor.shutdownNow();

        assertThat(supplierCalls.get()).isEqualTo(1);
        assertThat(ids).hasSize(1);
    }

    @Test
    public void should_round_robin_connections_in_pool() {
        RpcConnectionPool pool = new RpcConnectionPool();
        RpcConnection c1 = new RpcConnection("addr", new EmbeddedChannel());
        RpcConnection c2 = new RpcConnection("addr", new EmbeddedChannel());

        pool.add(c1);
        pool.add(c2);

        assertThat(pool.getActive().ch().id()).isEqualTo(c1.ch().id());
        assertThat(pool.getActive().ch().id()).isEqualTo(c2.ch().id());
        assertThat(pool.getActive().ch().id()).isEqualTo(c1.ch().id());

        pool.closeAll();
    }
}

