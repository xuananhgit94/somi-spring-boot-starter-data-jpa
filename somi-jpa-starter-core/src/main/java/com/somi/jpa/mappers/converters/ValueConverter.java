package com.somi.jpa.mappers.converters;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;

public interface ValueConverter {
    boolean supports(Object raw, Type targetType);

    @Nullable Object convert(Object raw, Type targetType);
}
