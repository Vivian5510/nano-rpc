package com.rosy.nano.transport.command;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Setter
public class RemotingCommand {

    private static final AtomicLong REQUEST_ID = new AtomicLong(0);

    private int code;

    private int version;

    @Setter(AccessLevel.NONE)
    private long opaque = REQUEST_ID.getAndIncrement();

    private int flag;

    private String remark;

    @Setter(AccessLevel.NONE)
    private HashMap<String, String> extFields;

    
}
