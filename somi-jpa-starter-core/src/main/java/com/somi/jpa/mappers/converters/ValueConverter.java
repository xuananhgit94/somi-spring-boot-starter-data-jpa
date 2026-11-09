package com.somi.jpa.mappers.converters;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;

public sealed interface ValueConverter
        permits NumberConverter, EnumConverter, StringConverter, DateTimeConverter, UuidConverter,
        JsonConverter, ArrayConverter, CollectionConverter, MapConverter {
    boolean supports(Object raw, Type targetType);

    @Nullable Object convert(Object raw, Type targetType);
}
