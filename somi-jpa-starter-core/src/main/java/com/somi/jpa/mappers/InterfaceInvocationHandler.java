package com.somi.jpa.mappers;

import com.somi.jpa.mappers.converters.ValueConversionService;
import org.jspecify.annotations.Nullable;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

record InterfaceInvocationHandler(Class<?> projectionType,
                                  Map<String, @Nullable Object> row,
                                  Map<Method, String> propertyKeys,
                                  ValueConversionService conversionService) implements InvocationHandler {

    @Override
    public @Nullable Object invoke(Object proxy, Method method, @Nullable Object @Nullable [] args) throws Throwable {
        if (ReflectionUtils.isEqualsMethod(method)) {
            return args != null && args[0] != null && Proxy.isProxyClass(args[0].getClass())
                    && Proxy.getInvocationHandler(args[0]) instanceof InterfaceInvocationHandler other
                    && projectionType == other.projectionType && row.equals(other.row);
        }
        if (ReflectionUtils.isHashCodeMethod(method)) {
            return row.hashCode();
        }
        if (ReflectionUtils.isToStringMethod(method)) {
            return projectionType.getSimpleName() + row.keySet();
        }
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }
        if (method.isDefault()) {
            return InvocationHandler.invokeDefault(proxy, method, args);
        }
        String key = propertyKeys.computeIfAbsent(method, m -> ObjectMappingStrategy.propertyKey(propertyName(m)));
        return conversionService.convert(row.get(key), method.getGenericReturnType());
    }

    private static String propertyName(Method method) {
        String name = method.getName();
        if (name.startsWith("get") && name.length() > 3) {
            return name.substring(3);
        }
        if (name.startsWith("is") && name.length() > 2) {
            return name.substring(2);
        }
        return name;
    }
}
