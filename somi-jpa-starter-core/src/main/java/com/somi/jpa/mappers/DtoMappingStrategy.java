package com.somi.jpa.mappers;

import com.somi.jpa.mappers.converters.ValueConversionService;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeanUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public final class DtoMappingStrategy<T> implements ObjectMappingStrategy<T> {
    private record PropertyWriter(Method setter, Type type) {}

    private final ValueConversionService conversionService;
    private final Constructor<T> constructor;
    private final Map<String, PropertyWriter> writers = new HashMap<>();

    DtoMappingStrategy(Class<T> targetClass, ValueConversionService conversionService) {
        this.conversionService = conversionService;
        try {
            this.constructor = targetClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("DTO needs a no-argument constructor: " + targetClass.getName(), e);
        }
        this.constructor.trySetAccessible();
        for (PropertyDescriptor descriptor : BeanUtils.getPropertyDescriptors(targetClass)) {
            Method setter = descriptor.getWriteMethod();
            if (setter != null) {
                writers.put(ObjectMappingStrategy.propertyKey(descriptor.getName()),
                        new PropertyWriter(setter, setter.getGenericParameterTypes()[0]));
            }
        }
    }

    @Override
    public T map(@Nullable Object[] tuple, String[] aliases) throws ReflectiveOperationException {
        T instance = constructor.newInstance();
        for (int i = 0; i < aliases.length; i++) {
            if (aliases[i] == null) continue;
            PropertyWriter writer = writers.get(ObjectMappingStrategy.propertyKey(aliases[i]));
            if (writer != null) {
                writer.setter().invoke(instance, conversionService.convert(tuple[i], writer.type()));
            }
        }
        return instance;
    }
}
