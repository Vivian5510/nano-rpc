package com.rosy.nano.protocol.rpc.connection;

import com.rosy.nano.transport.connection.Connection;
import io.netty.channel.Channel;

public class RpcConnection implements Connection {

    Channel ch;

    @Override
    public Channel ch() {
        return ch;
    }
}
