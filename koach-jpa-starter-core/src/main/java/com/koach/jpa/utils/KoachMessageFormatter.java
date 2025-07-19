package com.koach.jpa.utils;

import org.slf4j.helpers.MessageFormatter;

public class KoachMessageFormatter {

    public static String format(String template, Object... args) {
        if (template.contains("{}")) {
            return MessageFormatter.arrayFormat(template, args).getMessage();
        } else {
            return String.format(template, args);
        }
    }
}
