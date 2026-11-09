package com.koach.jpa.utils;

public class NamingUtils {
    private NamingUtils() {}

    public static String fromCamelCaseToSnakeCase(String camelCaseString) {
        if (camelCaseString == null || camelCaseString.isEmpty()) {
            return camelCaseString;
        }
        return camelCaseString.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
