package com.somi.jpa.utils;

public class TypeUtils {
    private TypeUtils() {}

    public static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type.isEnum()
                || type.getName().startsWith("java.lang.")
                || type.getName().startsWith("java.math.")
                || type.getName().startsWith("java.time.")
                || java.util.Date.class.isAssignableFrom(type)
                || type == java.util.UUID.class;
    }
}
