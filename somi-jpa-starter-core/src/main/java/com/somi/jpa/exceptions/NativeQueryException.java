package com.somi.jpa.exceptions;

import com.somi.jpa.enums.ErrorMessage;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSourceResolvable;

import java.io.Serial;

public class NativeQueryException extends RuntimeException implements MessageSourceResolvable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final ErrorMessage error;
    private final transient Object[] args;

    private NativeQueryException(ErrorMessage error, @Nullable Throwable cause, Object[] args) {
        Object[] arguments = args.clone();
        this.error = error;
        this.args = arguments;
        super(error.format(arguments), cause);
    }

    public static NativeQueryException of(ErrorMessage error, Object... args) {
        return new NativeQueryException(error, null, args);
    }

    public static NativeQueryException wrap(Throwable cause, ErrorMessage error, Object... args) {
        return new NativeQueryException(error, cause, args);
    }

    public ErrorMessage getError() {
        return error;
    }

    @Override
    public String[] getCodes() {
        return new String[]{error.getCode()};
    }

    @Override
    public Object[] getArguments() {
        return args.clone();
    }

    @Override
    public String getDefaultMessage() {
        return error.getDefaultTemplate();
    }
}
