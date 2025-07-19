package com.koach.jpa.exceptions;

import com.koach.jpa.configs.NativeQueryException;
import com.koach.jpa.enums.ErrorMessage;


public class DefaultNativeQueryException {
    private static MessageResolver messageResolver;
    public static void setMessageResolver(MessageResolver resolver) {
        DefaultNativeQueryException.messageResolver = resolver;
    }
    public static NativeQueryException of(Exception cause, ErrorMessage error, Object... args) {
        String message = messageResolver != null
                ? messageResolver.get(error, args)
                : error.format(args);
        return new NativeQueryException(message,  cause);
    }

    public static NativeQueryException of(ErrorMessage error, Object... args) {
        String message = messageResolver != null
                ? messageResolver.get(error, args)
                : error.format(args);
        return new NativeQueryException(message);
    }

    public static void fail(ErrorMessage error, Object... args) {
        throw of(error, args);
    }

    public static void fail(Exception cause, ErrorMessage error, Object... args) {
        throw of(cause, error, args);
    }
}
