package com.somi.jpa.mappers;

import com.somi.jpa.utils.ConverterUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DtoMappingStrategy<T> implements ObjectMappingStrategy<T>{
    private final Map<String, Field> fieldCache = new ConcurrentHashMap<>();
    private final Map<String, Method> setterCache = new ConcurrentHashMap<>();
    @Override
    public T map(Object[] tuple, String[] aliases, Class<T> targetClass) throws Exception {
        T instance = targetClass.getDeclaredConstructor().newInstance();
        Map<String, Object> dataMap = ConverterUtils.buildDataMap(tuple, aliases);
        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            String camelCaseAlias = toCamelCase(entry.getKey());
            Field field = getField(camelCaseAlias, targetClass);
            if (field != null) {
                Method setter = getSetter(field, targetClass);
                if (setter != null) {
                    Object convertedValue = ConverterUtils.convertValue(entry.getValue(), field.getType());
                    setter.invoke(instance, convertedValue);
                }// để ý để sửa lại sau khi test và debug
            }
        }
        return instance;
    }

    private Method getSetter(Field field, Class<T> targetClass) {
        return setterCache.computeIfAbsent(field.getName(), key -> {
            String setterName = "set" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
            try {
                return targetClass.getMethod(setterName, field.getType());
            } catch (NoSuchMethodException e) {
                return null;
            }
        });
    }

    private String toCamelCase(String s) {
        if (s == null) return null;
        if (!s.contains("_")) {
            if (Character.isUpperCase(s.charAt(0))) {
                return Character.toLowerCase(s.charAt(0)) + s.substring(1);
            }
            return s;
        }
        String[] parts = s.split("_");
        StringBuilder camelCaseString = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            camelCaseString.append(parts[i].substring(0, 1).toUpperCase()).append(parts[i].substring(1).toLowerCase());
        }
        return camelCaseString.toString();
    }

    private Field getField(String alias, Class<T> targetClass) {
        return fieldCache.computeIfAbsent(alias, key -> {
            for (Field field : targetClass.getDeclaredFields()) {
                if (field.getName().equalsIgnoreCase(key)) {
                    return field;
                }
            }
            return null;
        });
    }
}
