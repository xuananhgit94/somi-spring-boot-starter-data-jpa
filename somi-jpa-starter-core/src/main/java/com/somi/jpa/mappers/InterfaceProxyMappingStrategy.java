package com.somi.jpa.mappers;

import com.somi.jpa.mappers.converters.ValueConversionService;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps rows onto a JDK proxy of a projection interface, Spring Data style:
 * {@code getFullName()} reads column {@code fullname} or {@code full_name}.
 */
public final class InterfaceProxyMappingStrategy<T> implements ObjectMappingStrategy<T> {
    private final Class<T> targetClass;
    private final ValueConversionService conversionService;
    // Shared by all proxies of this query so property names are derived once per method
    private final Map<Method, String> propertyNames = new ConcurrentHashMap<>();

    InterfaceProxyMappingStrategy(Class<T> targetClass, ValueConversionService conversionService) {
        this.targetClass = targetClass;
        this.conversionService = conversionService;
    }

    @Override
    public T map(@Nullable Object[] tuple, String[] aliases) {
        Map<String, @Nullable Object> row = ObjectMappingStrategy.toRowMap(tuple, aliases);
        return targetClass.cast(Proxy.newProxyInstance(
                targetClass.getClassLoader(),
                new Class<?>[]{targetClass},
                new InterfaceInvocationHandler(targetClass, row, propertyNames, conversionService)
        ));
    }
}
