package com.rosy.nano.protocol.rpc.processor.rpc;

import com.google.common.base.Preconditions;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class UserProcessorRegistry {
    private final ConcurrentMap<String, UserProcessor> PROCESSORS = new ConcurrentHashMap<>();

    public void register(UserProcessor processor) {
        Preconditions.checkArgument(processor != null, "can't register null processor");
        Preconditions.checkState(processor.interest() != null && !processor.interest().isBlank(), "invalid processor interest");

        UserProcessor old = PROCESSORS.putIfAbsent(processor.interest(), processor);
        Preconditions.checkState(old == null, "duplicate processor registration for interest=%s", processor.interest());
    }

    public UserProcessor match(String interest) {
        return PROCESSORS.get(interest);
    }
}
