package com.somi.jpa.mappers;

import com.somi.jpa.utils.ConverterUtils;
import com.somi.jpa.utils.NamingUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.Map;
import java.util.stream.Stream;

public class RecordMappingStrategy<T> implements ObjectMappingStrategy<T> {
    @Override
    public T map(Object[] tuple, String[] aliases, Class<T> targetClass) throws Exception {
        RecordComponent[] components = targetClass.getRecordComponents();
        Class<?>[] paramTypes = Stream.of(components).map(RecordComponent::getType).toArray(Class<?>[]::new);
        Object[] args = new Object[components.length];
        Map<String, Object> dataMap = ConverterUtils.buildDataMap(tuple, aliases);
        for (int i = 0; i < components.length; i++) {
            String componentName = components[i].getName();
            Object rawValue = dataMap.get(componentName.toLowerCase());
            if (rawValue == null) {
                rawValue = dataMap.get(NamingUtils.fromCamelCaseToSnakeCase(componentName));
            }
            args[i] = ConverterUtils.convertValue(rawValue, components[i].getType());
        }
        Constructor<T> constructor = targetClass.getDeclaredConstructor(paramTypes);
        return constructor.newInstance(args);
    }
}
