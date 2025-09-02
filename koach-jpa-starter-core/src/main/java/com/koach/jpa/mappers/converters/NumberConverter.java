package com.koach.jpa.mappers.converters;

import java.lang.reflect.Type;


public class NumberConverter implements ValueConverter {
    @Override
    public boolean supports(Object raw, Type targetType) {
        return raw instanceof Number && targetType instanceof Class<?> cls && Number.class.isAssignableFrom(cls);
    }

    @Override
    public Object convert(Object raw, Type targetType) {
        if (!(targetType instanceof Class<?> cls)) return raw;
        Number n = (Number) raw;
        if (cls == Integer.class) return n.intValue();
        if (cls == Long.class) return n.longValue();
        if (cls == Double.class) return n.doubleValue();
        if (cls == Float.class) return n.floatValue();
        if (cls == Short.class) return n.shortValue();
        if (cls == Byte.class) return n.byteValue();
        return raw;
    }
}
