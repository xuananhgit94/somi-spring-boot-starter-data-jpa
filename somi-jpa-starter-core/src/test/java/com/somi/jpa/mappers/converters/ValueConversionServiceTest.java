package com.somi.jpa.mappers.converters;

import com.somi.jpa.exceptions.NativeQueryException;
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

class ValueConversionServiceTest {

    private final ValueConversionService conversion = ValueConversionService.withDefaults();

    enum Color { RED, GREEN }

    public static class Address {
        public String city;
    }

    @Test
    void nullStaysNull() {
        assertThat(conversion.convert(null, String.class)).isNull();
    }

    @Test
    void assignableValueIsReturnedAsIs() {
        Object raw = "abc";
        assertThat(conversion.convert(raw, CharSequence.class)).isSameAs(raw);
        assertThat(conversion.convert(5, int.class)).isEqualTo(5);
    }

    @Test
    void blankStringBecomesNullForNonStringTargets() {
        assertThat(conversion.convert("  ", Color.class)).isNull();
        assertThat(conversion.convert("", UUID.class)).isNull();
        assertThat(conversion.convert(" ", String.class)).isEqualTo(" ");
    }

    @Test
    void numbers() {
        assertThat(conversion.convert(5L, int.class)).isEqualTo(5);
        assertThat(conversion.convert(5, Long.class)).isEqualTo(5L);
        assertThat(conversion.convert(5L, BigDecimal.class)).isEqualTo(new BigDecimal("5"));
        assertThat(conversion.convert(new BigDecimal("7.9"), BigInteger.class)).isEqualTo(BigInteger.valueOf(7));
        assertThat(conversion.convert("12", Integer.class)).isEqualTo(12);
    }

    @Test
    void stringsToCharAndBoolean() {
        assertThat(conversion.convert("A", char.class)).isEqualTo('A');
        assertThat(conversion.convert("T", boolean.class)).isEqualTo(true);
        assertThat(conversion.convert("0", Boolean.class)).isEqualTo(false);
        assertThatThrownBy(() -> conversion.convert("AB", Character.class))
                .isInstanceOf(NativeQueryException.class);
    }

    @Test
    void enums() {
        assertThat(conversion.convert("green", Color.class)).isEqualTo(Color.GREEN);
        assertThatThrownBy(() -> conversion.convert("blue", Color.class))
                .isInstanceOf(NativeQueryException.class);
    }

    @Test
    void uuid() {
        UUID id = UUID.randomUUID();
        assertThat(conversion.convert(id.toString(), UUID.class)).isEqualTo(id);
        assertThatThrownBy(() -> conversion.convert("not-a-uuid", UUID.class))
                .isInstanceOf(NativeQueryException.class);
    }

    @Test
    void temporalFromJdbcTypes() {
        Timestamp ts = Timestamp.valueOf("2024-01-02 10:15:30");
        assertThat(conversion.convert(ts, LocalDateTime.class)).isEqualTo(LocalDateTime.of(2024, 1, 2, 10, 15, 30));
        assertThat(conversion.convert(ts, LocalDate.class)).isEqualTo(LocalDate.of(2024, 1, 2));
        assertThat(conversion.convert(ts, Instant.class)).isEqualTo(ts.toInstant());
        assertThat(conversion.convert(java.sql.Date.valueOf("2024-01-02"), LocalDate.class)).isEqualTo(LocalDate.of(2024, 1, 2));
        assertThat(conversion.convert(Time.valueOf("08:30:00"), LocalTime.class)).isEqualTo(LocalTime.of(8, 30));
        assertThat(conversion.convert(0L, Date.class)).isEqualTo(new Date(0));
        LocalDateTime local = LocalDateTime.of(2024, 1, 5, 10, 0);
        assertThat(conversion.convert(local, Date.class))
                .isEqualTo(Date.from(local.atZone(java.time.ZoneId.systemDefault()).toInstant()));
    }

    @Test
    void temporalFromStrings() {
        assertThat(conversion.convert("2024-01-02", LocalDate.class)).isEqualTo(LocalDate.of(2024, 1, 2));
        assertThat(conversion.convert("2024-01-02 10:15:30", LocalDateTime.class)).isEqualTo(LocalDateTime.of(2024, 1, 2, 10, 15, 30));
        assertThat(conversion.convert("2024-01-02T10:15:30", LocalDateTime.class)).isEqualTo(LocalDateTime.of(2024, 1, 2, 10, 15, 30));
        assertThat(conversion.convert("08:30", LocalTime.class)).isEqualTo(LocalTime.of(8, 30));
    }

    @Test
    void jsonStringToPojoAndCollections() {
        Object address = conversion.convert("{\"city\":\"Hanoi\"}", Address.class);
        assertThat(address).isInstanceOf(Address.class);
        assertThat(((Address) address).city).isEqualTo("Hanoi");
        assertThat(conversion.convert("[1,2]", List.class)).isEqualTo(List.of(1, 2));
        assertThat(conversion.convert("{\"a\":1}", Map.class)).isEqualTo(Map.of("a", 1));
        assertThat(conversion.convert(Map.of("city", "Hue"), Address.class))
                .extracting("city").isEqualTo("Hue");
    }

    @Test
    void invalidJsonFails() {
        assertThatThrownBy(() -> conversion.convert("{\"secret\":\"p@ss\"", Address.class))
                .isInstanceOf(NativeQueryException.class)
                .hasMessageContaining("Address")
                .hasMessageNotContaining("p@ss");
    }

    @Test
    void collectionToArray() {
        assertThat((String[]) conversion.convert(List.of("a", "b"), String[].class)).containsExactly("a", "b");
    }
}
