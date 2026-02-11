package com.rosy.nano.protocol.rpc.command;

public final class RpcResponseCode {

    public static final int SUCCESS = 0;
    public static final int BAD_REQUEST = 1;
    public static final int NO_PROCESSOR = 2;
    public static final int SYSTEM_BUSY = 3;
    public static final int INTERNAL_ERROR = 4;

    private RpcResponseCode() {
    }
}
