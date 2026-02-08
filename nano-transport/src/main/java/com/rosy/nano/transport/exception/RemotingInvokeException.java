package com.rosy.nano.transport.exception;


public class RemotingInvokeException extends RuntimeException {
    private final long opaque;
    private final String remoteAddr;
    private final int code;

    public RemotingInvokeException (long opaque, String remoteAddr, int code, String message, Throwable t) {
        super(message, t);
        this.opaque = opaque;
        this.remoteAddr = remoteAddr;
        this.code = code;
    }

    public long opaque() { return opaque; }
    public String remoteAddr() { return remoteAddr; }
    public int code() { return code; }
}
