package com.rosy.nano.transport.connection;

import io.netty.channel.Channel;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

public interface Connection {
    Channel ch();
}
