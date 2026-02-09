package com.rosy.nano.transport.exception;

public class RemotingSerializationException extends RuntimeException {
    public RemotingSerializationException(String message) {
        super(message);
    }

    public RemotingSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
