package com.koach.jpa.mappers.converters;

import java.lang.reflect.Type;

public interface ValueConverter {
    boolean supports(Object raw, Type targetType);
    Object convert(Object raw, Type targetType);
}
