package com.somi.jpa.mappers.converters;

import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.exceptions.NativeQueryException;

import java.lang.reflect.Type;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

/**
 * Converts JDBC and string values to {@code java.time} types. Legacy {@code java.util.Date} targets are not
 * produced here: a {@code Timestamp} is already a {@code Date}, other values go through Jackson.
 */
public class DateTimeConverter implements ValueConverter {
    @Override
    public boolean supports(Object raw, Type targetType) {
        if (!(targetType instanceof Class<?> cls)) return false;
        return cls == LocalDate.class
                || cls == LocalDateTime.class
                || cls == LocalTime.class
                || cls == Instant.class;
    }

    @Override
    public Object convert(Object raw, Type targetType) {
        Class<?> cls = (Class<?>) targetType;
        try {
            if (cls == LocalDate.class) return toLocalDateTime(raw).toLocalDate();
            if (cls == LocalDateTime.class) return toLocalDateTime(raw);
            if (cls == LocalTime.class) {
                if (raw instanceof Time t) return t.toLocalTime();
                if (raw instanceof String s && !s.contains("-")) return LocalTime.parse(s.trim());
                return toLocalDateTime(raw).toLocalTime();
            }
            return toInstant(raw);
        } catch (DateTimeParseException | IllegalArgumentException e) {
            throw NativeQueryException.wrap(e, ErrorMessage.CONVERT_VALUE_FAIL, raw.getClass().getSimpleName(), cls.getSimpleName());
        }
    }

    private LocalDateTime toLocalDateTime(Object raw) {
        return switch (raw) {
            case Timestamp ts -> ts.toLocalDateTime();
            case java.sql.Date d -> d.toLocalDate().atStartOfDay();
            case LocalDateTime ldt -> ldt;
            case LocalDate ld -> ld.atStartOfDay();
            case OffsetDateTime odt -> odt.toLocalDateTime();
            case ZonedDateTime zdt -> zdt.toLocalDateTime();
            case String s -> parseLocalDateTime(s.trim());
            default -> LocalDateTime.ofInstant(toInstant(raw), ZoneId.systemDefault());
        };
    }

    private Instant toInstant(Object raw) {
        return switch (raw) {
            case Timestamp ts -> ts.toInstant();
            // java.sql.Date.toInstant() throws UnsupportedOperationException
            case java.sql.Date d -> d.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
            case Instant i -> i;
            case OffsetDateTime odt -> odt.toInstant();
            case ZonedDateTime zdt -> zdt.toInstant();
            case LocalDateTime ldt -> ldt.atZone(ZoneId.systemDefault()).toInstant();
            case LocalDate ld -> ld.atStartOfDay(ZoneId.systemDefault()).toInstant();
            case Long l -> Instant.ofEpochMilli(l);
            case String s -> parseInstant(s.trim());
            default -> throw new IllegalArgumentException("Unsupported date/time value type: " + raw.getClass().getName());
        };
    }

    // DB drivers often render timestamps as "yyyy-MM-dd HH:mm:ss" instead of the ISO "T" separator
    private LocalDateTime parseLocalDateTime(String s) {
        if (s.length() == 10) return LocalDate.parse(s).atStartOfDay();
        return LocalDateTime.parse(s.replace(' ', 'T'));
    }

    private Instant parseInstant(String s) {
        try {
            return Instant.parse(s);
        } catch (DateTimeParseException _) {
            return parseLocalDateTime(s).atZone(ZoneId.systemDefault()).toInstant();
        }
    }
}
