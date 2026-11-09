package com.somi.jpa.exceptions;

import com.somi.jpa.enums.ErrorMessage;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSourceResolvable;

/**
 * Thrown for every failure of the native query starter.
 * <p>
 * Implements {@link MessageSourceResolvable} so applications can localize it with
 * {@code messageSource.getMessage(exception, locale)} by defining the {@link ErrorMessage#getCode() code}
 * in their own message bundle; {@link #getMessage()} always returns the English default.
 */
public class NativeQueryException extends RuntimeException implements MessageSourceResolvable {
    private final ErrorMessage error;
    private final Object[] args;

    private NativeQueryException(ErrorMessage error, @Nullable Throwable cause, Object[] args) {
        super(error.format(args), cause);
        this.error = error;
        this.args = args.clone();
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
