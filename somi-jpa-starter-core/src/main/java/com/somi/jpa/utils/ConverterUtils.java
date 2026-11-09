package com.somi.jpa.utils;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.type.TypeFactory;
import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.exceptions.DefaultNativeQueryException;
import com.somi.jpa.mappers.converters.ValueConverter;
import com.somi.jpa.mappers.converters.*;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Type;
import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConverterUtils {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private ConverterUtils() {
    }
    // Order matters: type-specific converters first; JsonConverter only claims non-simple targets
    private static final List<ValueConverter> VALUE_CONVERTERS = List.of(
            new NumberConverter(),
            new EnumConverter(),
            new StringConverter(),
            new DateTimeConverter(),
            new UuidConverter(),
            new JsonConverter(OBJECT_MAPPER),
            new ArrayConverter(OBJECT_MAPPER),
            new CollectionConverter(OBJECT_MAPPER),
            new MapConverter(OBJECT_MAPPER)
    );

    public static Map<String, Object> buildDataMap(Object[] tuple, String[] aliases) {
        Map<String, Object> dataMap = new HashMap<>();
        for (int i = 0; i < aliases.length; i++) {
            if (aliases[i] != null) {
                dataMap.put(aliases[i].toLowerCase(), tuple[i]);
            }
        }
        return dataMap;
    }

    public static Object convertValue(Object raw, Type targetType) {
        if (raw == null) return null;

        if (targetType instanceof Class<?> cls && ClassUtils.isAssignableValue(cls, raw)) {
            return raw;
        }
        if (raw instanceof String str && str.isBlank()) {
            return null;
        }
        Object value = raw instanceof Array sqlArray ? unwrapSqlArray(sqlArray) : raw;

        return VALUE_CONVERTERS.stream()
                .filter(c -> c.supports(value, targetType))
                .findFirst()
                .map(c -> c.convert(value, targetType))
                .orElseGet(() -> {
                    try {
                        return OBJECT_MAPPER.convertValue(
                                value,
                                OBJECT_MAPPER.getTypeFactory().constructType(targetType)
                        );
                    } catch (IllegalArgumentException e) {
                        throw DefaultNativeQueryException.of(e, ErrorMessage.CONVERT_VALUE_FAIL, value, targetType.getTypeName());
                    }
                });
    }

    private static List<Object> unwrapSqlArray(Array sqlArray) {
        try {
            return Arrays.asList((Object[]) sqlArray.getArray());
        } catch (SQLException e) {
            throw DefaultNativeQueryException.of(e, ErrorMessage.SQL_TO_ARRAY_FAIL);
        }
    }
}
