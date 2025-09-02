package com.koach.jpa.mappers.converters;

import java.lang.reflect.Type;
import java.util.UUID;

public class UuidConverter implements ValueConverter {

    @Override
    public boolean supports(Object raw, Type targetType) {
        return targetType == UUID.class;
    }

    @Override
    public Object convert(Object raw, Type targetType) {
        if (raw instanceof UUID) return raw;
        return UUID.fromString(raw.toString());
    }
}
