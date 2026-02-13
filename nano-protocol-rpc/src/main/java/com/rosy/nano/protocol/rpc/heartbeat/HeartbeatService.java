package com.rosy.nano.protocol.rpc.heartbeat;

import com.rosy.nano.protocol.rpc.command.RpcRequestCode;
import com.rosy.nano.protocol.rpc.command.header.HeartbeatRequestHeader;
import com.rosy.nano.transport.command.RemotingCommand;
import com.rosy.nano.transport.connection.Connection;
import com.rosy.nano.transport.lifecycle.LifeCycle;
import com.rosy.nano.transport.lifecycle.LifeCycleSupport;
import com.rosy.nano.transport.remoting.BaseRemoting;
import com.rosy.nano.transport.remoting.RemotingCallback;
import com.rosy.nano.transport.serialization.HeaderSerializeType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class HeartbeatService implements LifeCycle {
    public static final long HEARTBEAT_INTERVAL_MS = 30_000;
    public static final long HEARTBEAT_TIMEOUT_MS = 3_000;
    public static final long CHANNEL_IDLE_MS = 90_000;

    private final LifeCycleSupport SUPPORT = new LifeCycleSupport();
    private final ScheduledExecutorService scheduler;
    private final BaseRemoting remoting;
    private final HeaderSerializeType hType;
    private final Runnable scanner;

    private final ConcurrentHashMap<ChannelId, AtomicBoolean> INFLIGHT = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ChannelId, Channel> ACTIVES = new ConcurrentHashMap<>();

    public HeartbeatService(BaseRemoting remoting, HeaderSerializeType hType, Runnable scanner) {
        this.remoting = remoting;
        this.hType = hType;
        this.scanner = scanner;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rpc-heartbeat");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void launch() {
        SUPPORT.launch();
        scheduler.scheduleAtFixedRate(this::tick, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        SUPPORT.shutdown();
        scheduler.shutdown();
        INFLIGHT.clear();
        ACTIVES.clear();
    }

    public void onChannelActive(Channel channel) {
        if (channel == null) return;
        ACTIVES.put(channel.id(), channel);
        sendOnce(channel);
    }

    public void onChannelInactive(Channel channel) {
        if (channel == null) return;
        ACTIVES.remove(channel.id());
        INFLIGHT.remove(channel.id());
    }

    private void tick() {
        if (!SUPPORT.isLaunched()) return;
        for (Map.Entry<ChannelId, Channel> entry : ACTIVES.entrySet()) {
            sendOnce(entry.getValue());
        }
        if (scanner != null) {
            scanner.run();
        }
    }

    private void sendOnce(Channel channel) {
        if (channel == null) return;
        AtomicBoolean flag = INFLIGHT.computeIfAbsent(channel.id(), k -> new AtomicBoolean(false));
        if (!flag.compareAndSet(false, true)) return;
        sendHeartbeat(channel, () -> flag.set(false));
    }

    private void sendHeartbeat(Channel channel, Runnable onComplete) {
        if (channel == null || !channel.isActive()) {
            onComplete.run();
            return;
        }

        Connection conn = () -> channel;
        RemotingCommand heartbeat = buildHeartbeatRequest();
        remoting.invokeAsync(conn, heartbeat, TimeUnit.MILLISECONDS.toNanos(HEARTBEAT_TIMEOUT_MS),
            new RemotingCallback() {
                @Override
                public void onComplete() {
                    onComplete.run();
                }

                @Override
                public void onFailure(Throwable t) {
                    channel.close();
                }
            });
    }

    private RemotingCommand buildHeartbeatRequest() {
        HeartbeatRequestHeader header = new HeartbeatRequestHeader(System.currentTimeMillis());
        return RemotingCommand.newRequest(RpcRequestCode.RPC_HEARTBEAT, header, hType);
    }
}
