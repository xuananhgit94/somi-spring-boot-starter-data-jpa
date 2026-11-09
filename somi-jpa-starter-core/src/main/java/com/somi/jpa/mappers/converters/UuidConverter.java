package com.somi.jpa.mappers.converters;

import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.exceptions.NativeQueryException;

import java.lang.reflect.Type;
import java.util.UUID;

public final class UuidConverter implements ValueConverter {

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
            throw NativeQueryException.wrap(e, ErrorMessage.STRING_TO_UUID_FAIL);
        }
    }
}
