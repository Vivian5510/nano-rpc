package com.rosy.nano.transport.exception;

public class RemotingConnectException extends RuntimeException {

    public RemotingConnectException(String message) {
        super(message);
    }

    public RemotingConnectException(String message, Throwable cause) {
        super(message, cause);
    }
}
