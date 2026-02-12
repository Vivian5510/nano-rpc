package com.rosy.nano.protocol.rpc.exception;

public class RpcRemotingException extends RuntimeException {
    public RpcRemotingException(String message) {
        super(message);
    }

    public RpcRemotingException(String message, Throwable cause) {
        super(message, cause);
    }
}
