package com.rosy.nano.transport.event;

import io.netty.channel.Channel;

public record NettyEvent(NettyEventType type, Channel channel) {
}
