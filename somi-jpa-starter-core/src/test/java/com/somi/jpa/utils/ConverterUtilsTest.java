package com.somi.jpa.utils;

import com.somi.jpa.configs.NativeQueryException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConverterUtilsTest {

    enum Color { RED, GREEN }

    public static class Address {
        public String city;
    }

    @Test
    void nullStaysNull() {
        assertThat(ConverterUtils.convertValue(null, String.class)).isNull();
    }

    @Test
    void assignableValueIsReturnedAsIs() {
        Object raw = "abc";
        assertThat(ConverterUtils.convertValue(raw, CharSequence.class)).isSameAs(raw);
        assertThat(ConverterUtils.convertValue(5, int.class)).isEqualTo(5);
    }

    @Test
    void blankStringBecomesNullForNonStringTargets() {
        assertThat(ConverterUtils.convertValue("  ", Color.class)).isNull();
        assertThat(ConverterUtils.convertValue("", UUID.class)).isNull();
        assertThat(ConverterUtils.convertValue(" ", String.class)).isEqualTo(" ");
    }

    @Test
    void numbers() {
        assertThat(ConverterUtils.convertValue(5L, int.class)).isEqualTo(5);
        assertThat(ConverterUtils.convertValue(5, Long.class)).isEqualTo(5L);
        assertThat(ConverterUtils.convertValue(5L, BigDecimal.class)).isEqualTo(new BigDecimal("5"));
        assertThat(ConverterUtils.convertValue(new BigDecimal("7.9"), BigInteger.class)).isEqualTo(BigInteger.valueOf(7));
        assertThat(ConverterUtils.convertValue("12", Integer.class)).isEqualTo(12);
    }

    @Test
    void stringsToCharAndBoolean() {
        assertThat(ConverterUtils.convertValue("A", char.class)).isEqualTo('A');
        assertThat(ConverterUtils.convertValue("T", boolean.class)).isEqualTo(true);
        assertThat(ConverterUtils.convertValue("0", Boolean.class)).isEqualTo(false);
        assertThatThrownBy(() -> ConverterUtils.convertValue("AB", Character.class))
                .isInstanceOf(NativeQueryException.class);
    }

    @Test
    void enums() {
        assertThat(ConverterUtils.convertValue("green", Color.class)).isEqualTo(Color.GREEN);
        assertThatThrownBy(() -> ConverterUtils.convertValue("blue", Color.class))
                .isInstanceOf(NativeQueryException.class);
    }

    @Test
    void uuid() {
        UUID id = UUID.randomUUID();
        assertThat(ConverterUtils.convertValue(id.toString(), UUID.class)).isEqualTo(id);
        assertThatThrownBy(() -> ConverterUtils.convertValue("not-a-uuid", UUID.class))
                .isInstanceOf(NativeQueryException.class);
    }

    @Test
    void temporalFromJdbcTypes() {
        Timestamp ts = Timestamp.valueOf("2024-01-02 10:15:30");
        assertThat(ConverterUtils.convertValue(ts, LocalDateTime.class)).isEqualTo(LocalDateTime.of(2024, 1, 2, 10, 15, 30));
        assertThat(ConverterUtils.convertValue(ts, LocalDate.class)).isEqualTo(LocalDate.of(2024, 1, 2));
        assertThat(ConverterUtils.convertValue(ts, Instant.class)).isEqualTo(ts.toInstant());
        assertThat(ConverterUtils.convertValue(java.sql.Date.valueOf("2024-01-02"), LocalDate.class)).isEqualTo(LocalDate.of(2024, 1, 2));
        assertThat(ConverterUtils.convertValue(Time.valueOf("08:30:00"), LocalTime.class)).isEqualTo(LocalTime.of(8, 30));
        assertThat(ConverterUtils.convertValue(0L, Date.class)).isEqualTo(new Date(0));
    }

    @Test
    void temporalFromStrings() {
        assertThat(ConverterUtils.convertValue("2024-01-02", LocalDate.class)).isEqualTo(LocalDate.of(2024, 1, 2));
        assertThat(ConverterUtils.convertValue("2024-01-02 10:15:30", LocalDateTime.class)).isEqualTo(LocalDateTime.of(2024, 1, 2, 10, 15, 30));
        assertThat(ConverterUtils.convertValue("2024-01-02T10:15:30", LocalDateTime.class)).isEqualTo(LocalDateTime.of(2024, 1, 2, 10, 15, 30));
        assertThat(ConverterUtils.convertValue("08:30", LocalTime.class)).isEqualTo(LocalTime.of(8, 30));
    }

    @Test
    void jsonStringToPojoAndCollections() {
        Object address = ConverterUtils.convertValue("{\"city\":\"Hanoi\"}", Address.class);
        assertThat(address).isInstanceOf(Address.class);
        assertThat(((Address) address).city).isEqualTo("Hanoi");
        assertThat(ConverterUtils.convertValue("[1,2]", List.class)).isEqualTo(List.of(1, 2));
        assertThat(ConverterUtils.convertValue("{\"a\":1}", Map.class)).isEqualTo(Map.of("a", 1));
        assertThat(ConverterUtils.convertValue(Map.of("city", "Hue"), Address.class))
                .extracting("city").isEqualTo("Hue");
    }

    @Test
    void invalidJsonFails() {
        assertThatThrownBy(() -> ConverterUtils.convertValue("{oops", Address.class))
                .isInstanceOf(NativeQueryException.class);
    }

    @Test
    void collectionToArray() {
        assertThat((String[]) ConverterUtils.convertValue(List.of("a", "b"), String[].class)).containsExactly("a", "b");
    }
}
