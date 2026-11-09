package com.somi.jpa.mappers;

import com.somi.jpa.mappers.converters.ValueConversionService;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Maps one result row to an instance of a fixed target type. Implementations resolve reflection
 * metadata once, at construction, and are reused for every row of a query.
 */
public sealed interface ObjectMappingStrategy<T>
        permits DtoMappingStrategy, RecordMappingStrategy, InterfaceProxyMappingStrategy {

    T map(@Nullable Object[] tuple, String[] aliases) throws ReflectiveOperationException;

    static <T> ObjectMappingStrategy<T> of(Class<T> targetClass, ValueConversionService conversionService) {
        if (targetClass.isInterface()) return new InterfaceProxyMappingStrategy<>(targetClass, conversionService);
        if (targetClass.isRecord()) return new RecordMappingStrategy<>(targetClass, conversionService);
        return new DtoMappingStrategy<>(targetClass, conversionService);
    }

    /**
     * Indexes a row by lower-cased column alias.
     */
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
