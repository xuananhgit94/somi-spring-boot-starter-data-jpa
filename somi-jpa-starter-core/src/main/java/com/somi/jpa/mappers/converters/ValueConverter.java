package com.somi.jpa.mappers.converters;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;

/**
 * Converts a non-null JDBC value into a target Java type. Converters are tried in order and the first
 * one that {@link #supports supports} the pair wins.
 */
public interface ValueConverter {
    boolean supports(Object raw, Type targetType);

    @Nullable Object convert(Object raw, Type targetType);
}
