package com.rosy.nano.transport.event;

import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Slf4j
public class ChannelEventListenerChain implements ChannelEventListener {
    private final CopyOnWriteArrayList<ChannelEventListener> listeners = new CopyOnWriteArrayList<>();

    public ChannelEventListenerChain(ChannelEventListener... builtIns) {
        if(builtIns == null) return;

        for (ChannelEventListener builtIn : builtIns) {
            if(builtIn != null) listeners.add(builtIn);
        }
    }

    public void addEventListener (ChannelEventListener listener) {
        Preconditions.checkArgument(listener != null, "can't null listener");
        listeners.addIfAbsent(listener);
    }

    private void fire(Consumer<ChannelEventListener> action) {
        for (ChannelEventListener listener : listeners) {
            safeCall(listener, action);
        }
    }

    private void safeCall(ChannelEventListener listener, Consumer<ChannelEventListener> action) {
        try {
            action.accept(listener);
        } catch (Exception e) {
            log.warn("exception occurred when ChannelEventListener execute: {}", e.getMessage());
        }
    }

    @Override
    public void onChannelConnect(Channel channel) {
        fire(l -> l.onChannelConnect(channel));
    }

    @Override
    public void onChannelClose(Channel channel) {
        fire(l -> l.onChannelClose(channel));
    }

    @Override
    public void onChannelException(Channel channel) {
        fire(l -> l.onChannelException(channel));
    }

    @Override
    public void onChannelIdle(Channel channel) {
        fire(l -> l.onChannelIdle(channel));
    }

    @Override
    public void onChannelActive(Channel channel) {
        fire(l -> l.onChannelActive(channel));
    }
}
