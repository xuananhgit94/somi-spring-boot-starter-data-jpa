package com.koach.jpa.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.koach.jpa.enums.ErrorMessage;
import com.koach.jpa.exceptions.DefaultNativeQueryException;
import com.koach.jpa.mappers.converters.ValueConverter;
import com.koach.jpa.mappers.converters.*;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConverterUtils {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private ConverterUtils() {
    }
    private static final List<ValueConverter> VALUE_CONVERTERS = List.of(
            new NumberConverter(),
            new EnumConverter(),
            new JsonConverter(OBJECT_MAPPER),
            new StringConverter(),
            new DateTimeConverter(),
            new ArrayConverter(OBJECT_MAPPER),
            new UuidConverter(),
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

        if (targetType instanceof Class<?> cls && cls.isAssignableFrom(raw.getClass())) {
            return raw;
        }

        return VALUE_CONVERTERS.stream()
                .filter(c -> c.supports(raw, targetType))
                .findFirst()
                .map(c -> c.convert(raw, targetType))
                .orElseGet(() -> {
                    try {
                        return OBJECT_MAPPER.convertValue(
                                raw,
                                TypeFactory.defaultInstance().constructType(targetType)
                        );
                    } catch (Exception e) {
                        throw DefaultNativeQueryException.of(e, ErrorMessage.FAILED_TO_MAPPER, targetType.getTypeName());
                    }
                });
    }
}
