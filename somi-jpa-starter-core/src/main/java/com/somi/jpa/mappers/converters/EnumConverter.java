package com.somi.jpa.mappers.converters;

import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.exceptions.NativeQueryException;

import java.lang.reflect.Type;

public class EnumConverter implements ValueConverter {
    @Override
    public boolean supports(Object raw, Type targetType) {
        return raw instanceof String && targetType instanceof Class<?> cls && cls.isEnum();
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Object convert(Object raw, Type targetType) {
        String str = ((String) raw).trim();
        if (!(targetType instanceof Class<?> cls) || !cls.isEnum()) return raw;

        Class<? extends Enum> enumClass = cls.asSubclass(Enum.class);
        try {
            return Enum.valueOf(enumClass, str.toUpperCase());
        } catch (IllegalArgumentException e) {
            for (Enum<?> constant : enumClass.getEnumConstants()) {
                if (constant.name().equalsIgnoreCase(str)) {
                    return constant;
                }
            }
            throw NativeQueryException.wrap(e, ErrorMessage.STRING_TO_ENUM_FAIL, enumClass.getSimpleName());
        }
    }
}
