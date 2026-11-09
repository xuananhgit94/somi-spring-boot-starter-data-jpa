package com.somi.jpa.mappers.converters;
import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.exceptions.NativeQueryException;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;

public final class StringConverter implements ValueConverter {

    @Override
    public boolean supports(Object raw, Type targetType) {
        if (!(raw instanceof String)) return false;
        if (!(targetType instanceof Class<?> cls)) return false;

        return cls == String.class
                || cls == Character.class
                || cls == char.class
                || cls == Boolean.class
                || cls == boolean.class;
    }

    @Override
    public @Nullable Object convert(Object raw, Type targetType) {
        String str = ((String) raw).trim();
        if (str.isEmpty()) return null;

        Class<?> cls = (Class<?>) targetType;

        if (cls == Character.class || cls == char.class) {
            if (str.length() == 1) return str.charAt(0);
            throw NativeQueryException.of(ErrorMessage.STRING_TO_CHARACTER_FAIL, str.length());
        }

        if (cls == Boolean.class || cls == boolean.class) {
            return "1".equals(str) || "true".equalsIgnoreCase(str) || "T".equalsIgnoreCase(str);
        }

        return str;
    }
}
