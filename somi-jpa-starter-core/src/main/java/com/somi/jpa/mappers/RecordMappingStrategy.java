package com.somi.jpa.mappers;

import com.somi.jpa.mappers.converters.ValueConversionService;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.Map;
import java.util.stream.Stream;

public final class RecordMappingStrategy<T> implements ObjectMappingStrategy<T> {
    private final ValueConversionService conversionService;
    private final RecordComponent[] components;
    private final String[] keys;
    private final Constructor<T> constructor;

    RecordMappingStrategy(Class<T> targetClass, ValueConversionService conversionService) {
        this.conversionService = conversionService;
        this.components = targetClass.getRecordComponents();
        this.keys = Stream.of(components).map(c -> ObjectMappingStrategy.propertyKey(c.getName())).toArray(String[]::new);
        Class<?>[] paramTypes = Stream.of(components).map(RecordComponent::getType).toArray(Class<?>[]::new);
        try {
            this.constructor = targetClass.getDeclaredConstructor(paramTypes);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Record has no canonical constructor: " + targetClass.getName(), e);
        }
        this.constructor.trySetAccessible();
    }

    @Override
    public T map(@Nullable Object[] tuple, String[] aliases) throws ReflectiveOperationException {
        Map<String, @Nullable Object> row = ObjectMappingStrategy.toRowMap(tuple, aliases);
        @Nullable Object[] args = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            args[i] = conversionService.convert(row.get(keys[i]), components[i].getGenericType());
        }
        return constructor.newInstance(args);
    }
}
