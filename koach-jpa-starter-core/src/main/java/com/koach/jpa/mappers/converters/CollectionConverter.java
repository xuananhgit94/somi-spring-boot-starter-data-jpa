package com.koach.jpa.mappers.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.lang.reflect.Type;
import java.util.Collection;

public record CollectionConverter(ObjectMapper objectMapper) implements ValueConverter {

    @Override
    public boolean supports(Object raw, Type targetType) {
        if (!(targetType instanceof Class<?> cls)) return false;
        return Collection.class.isAssignableFrom(cls);
    }

    @Override
    public Object convert(Object raw, Type targetType) {
        return objectMapper.convertValue(raw,
                TypeFactory.defaultInstance().constructType(targetType));
    }
}
