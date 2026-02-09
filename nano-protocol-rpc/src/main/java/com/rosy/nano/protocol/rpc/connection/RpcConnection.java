package com.rosy.nano.transport.connection;

import io.netty.channel.Channel;

public class DefaultConnection implements Connection {

    Channel ch;

    @Override
    public Channel ch() {
        return ch;
    }
}
