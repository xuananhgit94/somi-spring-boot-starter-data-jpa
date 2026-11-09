package com.somi.jpa.mappers.converters;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.type.TypeFactory;

import java.lang.reflect.Type;

public record ArrayConverter(ObjectMapper objectMapper) implements ValueConverter {

    @Override
    public boolean supports(Object raw, Type targetType) {
        return targetType instanceof Class<?> cls && cls.isArray();
    }

    @Override
    public Object convert(Object raw, Type targetType) {
        return objectMapper.convertValue(raw,
                objectMapper.getTypeFactory().constructType(targetType));
    }
}
