package com.rosy.nano.transport.exception;

public class RemotingInvokeInterruptedException extends RemotingInvokeException {
    public RemotingInvokeInterruptedException(long opaque, String remoteAddr, int code, String message, Throwable t) {
        super(opaque, remoteAddr, code, message, t);
    }
}
