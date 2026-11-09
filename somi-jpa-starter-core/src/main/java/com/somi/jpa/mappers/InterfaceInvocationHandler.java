package com.somi.jpa.mappers;

import com.somi.jpa.mappers.converters.ValueConversionService;
import com.somi.jpa.utils.NamingUtils;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Locale;
import java.util.Map;

record InterfaceInvocationHandler(Class<?> projectionType,
                                  Map<String, @Nullable Object> row,
                                  Map<Method, String> propertyNames,
                                  ValueConversionService conversionService) implements InvocationHandler {

    @Override
    public @Nullable Object invoke(Object proxy, Method method, @Nullable Object @Nullable [] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return switch (method.getName()) {
                case "equals" -> args != null && args[0] != null && Proxy.isProxyClass(args[0].getClass())
                        && Proxy.getInvocationHandler(args[0]) instanceof InterfaceInvocationHandler other
                        && projectionType == other.projectionType && row.equals(other.row);
                case "hashCode" -> row.hashCode();
                case "toString" -> projectionType.getSimpleName() + row;
                default -> method.invoke(this, args);
            };
        }
        if (method.isDefault()) {
            return InvocationHandler.invokeDefault(proxy, method, args);
        }
        String propertyName = propertyNames.computeIfAbsent(method, InterfaceInvocationHandler::propertyName);
        Object rawValue = row.get(propertyName.toLowerCase(Locale.ROOT));
        if (rawValue == null) {
            rawValue = row.get(NamingUtils.fromCamelCaseToSnakeCase(propertyName));
        }
        return conversionService.convert(rawValue, method.getGenericReturnType());
    }

    private static String propertyName(Method method) {
        String name = method.getName();
        if (name.startsWith("get") && name.length() > 3) {
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        }
        if (name.startsWith("is") && name.length() > 2) {
            return Character.toLowerCase(name.charAt(2)) + name.substring(3);
        }
        return name;
    }
}
