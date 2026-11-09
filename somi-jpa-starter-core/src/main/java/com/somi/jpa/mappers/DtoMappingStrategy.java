package com.somi.jpa.mappers;

import com.somi.jpa.mappers.converters.ValueConversionService;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Maps rows onto a mutable class through its public setters. Column {@code full_name} or
 * {@code fullName} is written with {@code setFullName(..)}.
 */
public final class DtoMappingStrategy<T> implements ObjectMappingStrategy<T> {
    private record PropertyWriter(Method setter, Type type) {}

    private final ValueConversionService conversionService;
    private final Constructor<T> constructor;
    // Keyed by propertyKey(..), so lookups ignore case and underscores
    private final Map<String, PropertyWriter> writers = new HashMap<>();

    DtoMappingStrategy(Class<T> targetClass, ValueConversionService conversionService) {
        this.conversionService = conversionService;
        try {
            this.constructor = targetClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("DTO needs a no-argument constructor: " + targetClass.getName(), e);
        }
        this.constructor.trySetAccessible();
        for (Class<?> type = targetClass; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                Method setter = findSetter(targetClass, field);
                if (setter != null) {
                    writers.putIfAbsent(propertyKey(field.getName()),
                            new PropertyWriter(setter, setter.getGenericParameterTypes()[0]));
                }
            }
        }
    }

    @Override
    public T map(@Nullable Object[] tuple, String[] aliases) throws ReflectiveOperationException {
        T instance = constructor.newInstance();
        for (int i = 0; i < aliases.length; i++) {
            if (aliases[i] == null) continue;
            PropertyWriter writer = writers.get(propertyKey(aliases[i]));
            if (writer != null) {
                writer.setter().invoke(instance, conversionService.convert(tuple[i], writer.type()));
            }
        }
        return instance;
    }

    /** {@code FULL_NAME}, {@code full_name} and {@code fullName} all become {@code fullname}. */
    private static String propertyKey(String name) {
        return name.replace("_", "").toLowerCase(Locale.ROOT);
    }

    private static @Nullable Method findSetter(Class<?> targetClass, Field field) {
        String name = field.getName();
        String setterName = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        try {
            return targetClass.getMethod(setterName, field.getType());
        } catch (NoSuchMethodException _) {
            return null;
        }
    }
}
