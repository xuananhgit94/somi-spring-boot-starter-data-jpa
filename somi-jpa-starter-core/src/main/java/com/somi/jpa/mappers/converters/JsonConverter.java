package com.somi.jpa.mappers.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.exceptions.DefaultNativeQueryException;
import com.somi.jpa.utils.TypeUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public record JsonConverter(ObjectMapper objectMapper) implements ValueConverter {

    @Override
    public boolean supports(Object raw, Type targetType) {
        if (raw == null || TypeUtils.isSimpleType(TypeFactory.rawClass(targetType))) return false;
        return (raw instanceof String)
                || isPgObject(raw)
                || raw instanceof Map
                || raw instanceof Collection;
    }

    @Override
    public Object convert(Object raw, Type targetType) {
        if (raw instanceof Map || raw instanceof Collection) {
            try {
                return objectMapper.convertValue(raw, TypeFactory.defaultInstance().constructType(targetType));
            } catch (IllegalArgumentException e) {
                throw DefaultNativeQueryException.of(e, ErrorMessage.CONVERT_VALUE_FAIL, raw, targetType.getTypeName());
            }
        }
        String json = raw instanceof String str ? str : extractPgObjectValue(raw);
        try {
            return objectMapper.readValue(json, TypeFactory.defaultInstance().constructType(targetType));
        } catch (Exception e) {
            throw DefaultNativeQueryException.of(e, ErrorMessage.STRING_TO_JSON_FAIL, targetType.getTypeName(), json);
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

    private String extractPgObjectValue(Object pgObject) {
        try {
            Method getValue = pgObject.getClass().getMethod("getValue");
            return (String) getValue.invoke(pgObject);
        } catch (Exception e) {
            throw DefaultNativeQueryException.of(e, ErrorMessage.STRING_TO_JSON_FAIL, "PGobject", pgObject);
        }
    }
}
