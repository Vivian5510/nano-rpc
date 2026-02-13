package com.rosy.nano.protocol.rpc.event;

import com.rosy.nano.protocol.rpc.heartbeat.HeartbeatService;
import com.rosy.nano.transport.event.ChannelEventListener;
import io.netty.channel.Channel;

public final class ClientChannelEventListener implements ChannelEventListener {
    private final HeartbeatService heartbeatService;

    public ClientChannelEventListener(HeartbeatService heartbeatService) {
        this.heartbeatService = heartbeatService;
    }

    @Override
    public void onChannelConnect(Channel channel) {
    }

    @Override
    public void onChannelClose(Channel channel) {
        heartbeatService.onChannelInactive(channel);
    }

    @Override
    public void onChannelException(Channel channel) {
        heartbeatService.onChannelInactive(channel);
    }

    @Override
    public void onChannelIdle(Channel channel) {
        heartbeatService.onChannelInactive(channel);
    }

    @Override
    public void onChannelActive(Channel channel) {
        heartbeatService.onChannelActive(channel);
    }
}

