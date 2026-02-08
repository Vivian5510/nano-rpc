package com.rosy.nano.transport.exception;

public class RemotingLifecycleException extends RuntimeException {

    public RemotingLifecycleException(String message) {
        super(message);
    }

    public RemotingLifecycleException(String message, Throwable cause) {
        super(message, cause);
    }
}
