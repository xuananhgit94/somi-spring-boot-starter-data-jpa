package com.koach.jpa.mappers.converters;

import com.koach.jpa.enums.ErrorMessage;
import com.koach.jpa.exceptions.DefaultNativeQueryException;

import java.lang.reflect.Type;
import java.util.UUID;

public class UuidConverter implements ValueConverter {

    @Override
    public boolean supports(Object raw, Type targetType) {
        return targetType == UUID.class;
    }

    @Override
    public Object convert(Object raw, Type targetType) {
        String str = raw.toString().trim();
        try {
            return UUID.fromString(str);
        } catch (IllegalArgumentException e) {
            throw DefaultNativeQueryException.of(e, ErrorMessage.STRING_TO_UUID_FAIL, str);
        }
    }
}
