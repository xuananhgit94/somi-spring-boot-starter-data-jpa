package com.somi.jpa.mappers.converters;

import org.springframework.util.ClassUtils;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;


public class NumberConverter implements ValueConverter {
    @Override
    public boolean supports(Object raw, Type targetType) {
        return raw instanceof Number && targetType instanceof Class<?> cls
                && Number.class.isAssignableFrom(ClassUtils.resolvePrimitiveIfNecessary(cls));
    }

    @Override
    public Object convert(Object raw, Type targetType) {
        Class<?> cls = ClassUtils.resolvePrimitiveIfNecessary((Class<?>) targetType);
        Number n = (Number) raw;
        if (cls == Integer.class) return n.intValue();
        if (cls == Long.class) return n.longValue();
        if (cls == Double.class) return n.doubleValue();
        if (cls == Float.class) return n.floatValue();
        if (cls == Short.class) return n.shortValue();
        if (cls == Byte.class) return n.byteValue();
        if (cls == BigDecimal.class) return new BigDecimal(n.toString());
        if (cls == BigInteger.class) {
            return n instanceof BigDecimal bd ? bd.toBigInteger() : BigInteger.valueOf(n.longValue());
        }
        return raw;
    }
}
