package com.rosy.nano.transport.event;

import io.netty.channel.Channel;

public interface ChannelEventListener {
    default void onChannelConnect(Channel channel) {}

    default void onChannelClose(Channel channel) {}

    default void onChannelException(Channel channel) {}

    default void onChannelIdle(Channel channel) {}

    default void onChannelActive(Channel channel) {}
}
