package com.somi.jpa.mappers;

import com.somi.jpa.utils.ConverterUtils;
import com.somi.jpa.utils.NamingUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InterfaceInvocationHandler implements InvocationHandler {
    private final Map<String, Object> dataMap;
    private final Map<Method, String> methodToPropertyNameCache = new ConcurrentHashMap<>();
    public InterfaceInvocationHandler(Map<String, Object> dataMap) {
        this.dataMap = dataMap;
    }
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }
        String propertyName = getPropertyNameFromMethod(method);
        Object rawValue = dataMap.get(propertyName.toLowerCase());

        if (rawValue == null) {
            String snakeCasePropertyName = NamingUtils.fromCamelCaseToSnakeCase(propertyName);
            rawValue = dataMap.get(snakeCasePropertyName);
        }
        return ConverterUtils.convertValue(rawValue, method.getReturnType());
    }
    private String getPropertyNameFromMethod(Method method) {
        return methodToPropertyNameCache.computeIfAbsent(method, m -> {
            String methodName = m.getName();
            if (methodName.startsWith("get") && methodName.length() > 3) {
                return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
            } else if (methodName.startsWith("is") && methodName.length() > 2) {
                return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
            }
            return methodName;
        });
    }
}
