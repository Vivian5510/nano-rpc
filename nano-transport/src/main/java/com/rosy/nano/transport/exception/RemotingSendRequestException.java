package com.rosy.nano.transport.exception;

public class RemotingSendRequestException extends RemotingInvokeException {
    public RemotingSendRequestException(long opaque, String remoteAddr, int code, String message, Throwable t) {
        super(opaque, remoteAddr, code, message, t);
    }
}
