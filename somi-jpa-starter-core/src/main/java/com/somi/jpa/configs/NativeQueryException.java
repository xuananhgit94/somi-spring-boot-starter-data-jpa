package com.somi.jpa.configs;

public class NativeQueryException extends RuntimeException {
    public NativeQueryException(String message, Throwable cause) {
        super(message, cause);
    }

    public NativeQueryException(String message) {
        super(message);
    }
}
