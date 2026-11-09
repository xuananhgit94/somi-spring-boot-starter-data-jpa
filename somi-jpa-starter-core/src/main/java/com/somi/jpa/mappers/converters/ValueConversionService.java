package com.somi.jpa.mappers.converters;

import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.exceptions.NativeQueryException;
import org.jspecify.annotations.Nullable;
import org.springframework.util.ClassUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Type;
import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class ValueConversionService {
    private final ObjectMapper objectMapper;
    private final List<ValueConverter> converters;

    public ValueConversionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.converters = List.of(
                new NumberConverter(),
                new EnumConverter(),
                new StringConverter(),
                new DateTimeConverter(),
                new UuidConverter(),
                new JsonConverter(objectMapper),
                new ArrayConverter(objectMapper),
                new CollectionConverter(objectMapper),
                new MapConverter(objectMapper)
        );
    }

    public static ValueConversionService withDefaults() {
        return new ValueConversionService(JsonMapper.builder().build());
    }

    public @Nullable Object convert(@Nullable Object raw, Type targetType) {
        return switch (raw) {
            case null -> null;
            case Object value when targetType instanceof Class<?> cls && ClassUtils.isAssignableValue(cls, value) -> value;
            case String text when text.isBlank() -> null;
            case Array sqlArray -> convertValue(unwrapSqlArray(sqlArray), targetType);
            default -> convertValue(raw, targetType);
        };
    }

    private @Nullable Object convertValue(Object value, Type targetType) {
        for (ValueConverter converter : converters) {
            if (converter.supports(value, targetType)) {
                return converter.convert(value, targetType);
            }
        }
        try {
            return objectMapper.convertValue(value, objectMapper.getTypeFactory().constructType(targetType));
        } catch (JacksonException | IllegalArgumentException e) {
            throw NativeQueryException.wrap(e, ErrorMessage.CONVERT_VALUE_FAIL,
                    value.getClass().getSimpleName(), targetType.getTypeName());
        }
    }

    private static List<Object> unwrapSqlArray(Array sqlArray) {
        try {
            return Arrays.asList((Object[]) sqlArray.getArray());
        } catch (SQLException e) {
            throw NativeQueryException.wrap(e, ErrorMessage.SQL_TO_ARRAY_FAIL);
        }
    }
}
