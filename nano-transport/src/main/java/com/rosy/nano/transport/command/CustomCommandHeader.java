package com.rosy.nano.transport.command;

import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.Map;

public abstract class CustomCommandHeader {

    protected void validate() {
    }

    protected abstract void encodeTo(Map<String, String> extFields);

    protected abstract void decodeFrom(Map<String, String> extFields);

    public final void encodeTo0(Map<String, String> extFields) {
        Preconditions.checkArgument(extFields != null, "extFields can't be null");
        validate();
        encodeTo(extFields);
    }

    public final void decodeFrom0(Map<String, String> extFields) {
        Map<String, String> safe = extFields != null ? extFields : Collections.emptyMap();
        decodeFrom(safe);
        validate();
    }

    protected final String requireExt(Map<String, String> extFields, String key) {
        String value = extFields.get(key);
        Preconditions.checkArgument(value != null, "missing %s", key);
        return value;
    }
}
