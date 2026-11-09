package com.somi.jpa.mappers.converters;

import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;
import java.util.Map;

public record MapConverter(ObjectMapper objectMapper) implements ValueConverter {

    @Override
    public boolean supports(Object raw, Type targetType) {
        if (!(targetType instanceof Class<?> cls)) return false;
        return Map.class.isAssignableFrom(cls);
    }

    @Override
    public Object convert(Object raw, Type targetType) {
        return objectMapper.convertValue(raw,
                objectMapper.getTypeFactory().constructType(targetType));
    }
}
