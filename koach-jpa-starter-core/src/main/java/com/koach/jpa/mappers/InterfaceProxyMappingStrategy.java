package com.koach.jpa.mappers;

import com.koach.jpa.utils.ConverterUtils;

import java.lang.reflect.Proxy;
import java.util.Map;
public record InterfaceProxyMappingStrategy<T>() implements ObjectMappingStrategy<T> {
    @SuppressWarnings("unchecked")
    @Override
    public T map(Object[] tuple, String[] aliases, Class<T> targetClass) {
        Map<String, Object> dataMap = ConverterUtils.buildDataMap(tuple, aliases);
        return (T) Proxy.newProxyInstance(
                targetClass.getClassLoader(),
                new Class<?>[]{targetClass},
                new InterfaceInvocationHandler(dataMap)
        );
    }
}
