package com.koach.jpa.exceptions;

import com.koach.jpa.enums.ErrorMessage;
import com.koach.jpa.utils.KoachMessageFormatter;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

public class MessageResolver {

    private final MessageSource messageSource;

    public MessageResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String get(ErrorMessage error, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();

        try {
            String template = messageSource.getMessage(error.getCode(), null, locale);
            return KoachMessageFormatter.format(template, args);
        } catch (Exception e) {
            return KoachMessageFormatter.format(error.getDefaultTemplate(), args);
        }
    }
}

