package com.koach.jpa.utils;

public class TypeUtils {
    private TypeUtils() {}

    public static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type.getName().startsWith("java.lang.")
                || type == java.util.Date.class
                || type == java.time.LocalDate.class
                || type == java.time.LocalDateTime.class
                || type == java.time.LocalTime.class
                || type == java.util.UUID.class;
    }
}
