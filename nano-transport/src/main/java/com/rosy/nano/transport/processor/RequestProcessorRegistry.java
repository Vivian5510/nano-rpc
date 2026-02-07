package com.rosy.nano.transport.processor;

public interface RequestProcessorRegistry {

    RequestProcessor match(int code);

    void register(int code, RequestProcessor processor);

}
