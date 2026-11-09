package com.somi.jpa.exceptions;

import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.utils.SomiMessageFormatter;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

public record MessageResolver(MessageSource messageSource) {

    public String get(ErrorMessage error, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();

        try {
            String template = messageSource.getMessage(error.getCode(), null, locale);
            return SomiMessageFormatter.format(template, args);
        } catch (Exception e) {
            return SomiMessageFormatter.format(error.getDefaultTemplate(), args);
        }
    }
}

