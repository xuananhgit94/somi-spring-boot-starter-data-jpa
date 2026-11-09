package com.somi.jpa.mappers.converters;

import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.exceptions.NativeQueryException;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Stream;

public final class EnumConverter implements ValueConverter {
    @Override
    public boolean supports(Object raw, Type targetType) {
        return raw instanceof String && targetType instanceof Class<?> cls && cls.isEnum();
    }

    @Override
    public Object convert(Object raw, Type targetType) {
        String value = raw.toString().trim();
        Class<?> enumClass = (Class<?>) targetType;
        return Stream.ofNullable(enumClass.getEnumConstants())
                .flatMap(Arrays::stream)
                .filter(constant -> constant instanceof Enum<?> e && e.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> NativeQueryException.of(ErrorMessage.STRING_TO_ENUM_FAIL, enumClass.getSimpleName()));
    }
}
