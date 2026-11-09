package com.somi.jpa.mappers;

import com.somi.jpa.mappers.converters.ValueConversionService;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public sealed interface ObjectMappingStrategy<T>
        permits DtoMappingStrategy, RecordMappingStrategy, InterfaceProxyMappingStrategy {

    T map(@Nullable Object[] tuple, String[] aliases) throws ReflectiveOperationException;

    static <T> ObjectMappingStrategy<T> of(Class<T> targetClass, ValueConversionService conversionService) {
        if (targetClass.isInterface()) return new InterfaceProxyMappingStrategy<>(targetClass, conversionService);
        if (targetClass.isRecord()) return new RecordMappingStrategy<>(targetClass, conversionService);
        return new DtoMappingStrategy<>(targetClass, conversionService);
    }

    static Map<String, @Nullable Object> toRowMap(@Nullable Object[] tuple, String[] aliases) {
        Map<String, @Nullable Object> row = HashMap.newHashMap(aliases.length);
        for (int i = 0; i < aliases.length; i++) {
            if (aliases[i] != null) {
                row.put(aliases[i].toLowerCase(Locale.ROOT), tuple[i]);
            }
        }
        return row;
    }
}
