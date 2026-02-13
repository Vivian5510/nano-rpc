package com.rosy.nano.transport.event;

import io.netty.channel.Channel;

public interface ChannelEventListener {
    void onChannelConnect(Channel channel);

    void onChannelClose(Channel channel);

    void onChannelException(Channel channel);

    void onChannelIdle(Channel channel);

    void onChannelActive(Channel channel);
}
