package com.rosy.nano.transport.exception;

public class RemotingInvokeTimeoutException extends RemotingInvokeException {
    public RemotingInvokeTimeoutException(long opaque, String remoteAddr, int code, String message, Throwable t) {
        super(opaque, remoteAddr, code, message, t);
    }
}
