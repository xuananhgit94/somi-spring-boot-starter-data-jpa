package com.koach.jpa.mappers.converters;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateTimeConverter implements ValueConverter {
    @Override
    public boolean supports(Object raw, Type targetType) {
        if (!(targetType instanceof Class<?> cls)) return false;
        return cls == Date.class
                || cls == LocalDate.class
                || cls == LocalDateTime.class
                || cls == LocalTime.class
                || cls == Instant.class;
    }

    @Override
    public Object convert(Object raw, Type targetType) {
        Class<?> cls = (Class<?>) targetType;
        if (cls == Date.class) {
            if (raw instanceof java.sql.Timestamp ts) return new Date(ts.getTime());
            if (raw instanceof Long l) return new Date(l);
        }
        if (cls == LocalDate.class) return LocalDate.parse(raw.toString(), DateTimeFormatter.ISO_DATE);
        if (cls == LocalDateTime.class) return LocalDateTime.parse(raw.toString(), DateTimeFormatter.ISO_DATE_TIME);
        if (cls == LocalTime.class) return LocalTime.parse(raw.toString(), DateTimeFormatter.ISO_TIME);
        if (cls == Instant.class) return Instant.parse(raw.toString());
        return raw;
    }
}

