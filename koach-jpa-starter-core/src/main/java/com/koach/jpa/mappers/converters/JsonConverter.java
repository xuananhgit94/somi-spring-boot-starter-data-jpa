package com.koach.jpa.mappers.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public record JsonConverter(ObjectMapper objectMapper) implements ValueConverter {

    @Override
    public boolean supports(Object raw, Type targetType) {
        if (raw == null) return false;
        Class<?> rawClass = raw.getClass();
        return (raw instanceof String)
                || isPgObject(raw)
                || Map.class.isAssignableFrom(rawClass)
                || Collection.class.isAssignableFrom(rawClass);
    }

    @Override
    public Object convert(Object raw, Type targetType) {
        try {
            if (raw instanceof String str) {
                return objectMapper.readValue(str,
                        TypeFactory.defaultInstance().constructType(targetType));
            }
            if (isPgObject(raw)) {
                String json = extractPgObjectValue(raw);
                return objectMapper.readValue(json,
                        TypeFactory.defaultInstance().constructType(targetType));
            }
            if (raw instanceof Map || raw instanceof Collection) {
                return objectMapper.convertValue(raw,
                        TypeFactory.defaultInstance().constructType(targetType));
            }
            return raw;
        } catch (Exception e) {
            throw new RuntimeException("JsonConverter failed for " + targetType, e);
        }
    }

    private boolean isPgObject(Object raw) {
        if (!"org.postgresql.util.PGobject".equals(raw.getClass().getName())) return false;
        try {
            Method getType = raw.getClass().getMethod("getType");
            String type = (String) getType.invoke(raw);
            return "json".equalsIgnoreCase(type) || "jsonb".equalsIgnoreCase(type);
        } catch (Exception e) {
            return false;
        }
    }

    private String extractPgObjectValue(Object pgObject) throws Exception {
        Method getValue = pgObject.getClass().getMethod("getValue");
        return (String) getValue.invoke(pgObject);
    }
}
