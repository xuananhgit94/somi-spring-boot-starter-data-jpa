package com.somi.jpa.mappers;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UniversalResultTransformerTest {

    private static final String[] ALIASES = {"ID", "FULL_NAME", "SALARY", "CREATED_AT"};
    private static final Object[] TUPLE = {1L, "Nguyen Van A", new BigDecimal("1000.50"), Timestamp.valueOf("2024-01-02 10:15:30")};

    public record PersonRecord(Long id, String fullName, BigDecimal salary, LocalDateTime createdAt) {}

    public interface PersonView {
        Long getId();
        String getFullName();
        LocalDateTime getCreatedAt();
    }

    public static class PersonDto {
        private Long id;
        private String fullName;
        private double salary;
        private LocalDateTime createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public double getSalary() { return salary; }
        public void setSalary(double salary) { this.salary = salary; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    @Test
    void mapsRecordUsingSnakeCaseAliases() {
        var transformer = new UniversalResultTransformer<>(PersonRecord.class, false);

        PersonRecord result = transformer.transformTuple(TUPLE, ALIASES);

        assertThat(result).isEqualTo(new PersonRecord(1L, "Nguyen Van A", new BigDecimal("1000.50"),
                LocalDateTime.of(2024, 1, 2, 10, 15, 30)));
    }

    @Test
    void mapsDtoThroughSetters() {
        var transformer = new UniversalResultTransformer<>(PersonDto.class, false);

        PersonDto first = transformer.transformTuple(TUPLE, ALIASES);
        PersonDto second = transformer.transformTuple(TUPLE, ALIASES);

        assertThat(first.getId()).isEqualTo(1L);
        assertThat(first.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(first.getSalary()).isEqualTo(1000.50);
        assertThat(first.getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 2, 10, 15, 30));
        assertThat(second.getFullName()).isEqualTo(first.getFullName());
    }

    @Test
    void mapsInterfaceProjection() {
        var transformer = new UniversalResultTransformer<>(PersonView.class, true);

        PersonView view = transformer.transformTuple(TUPLE, ALIASES);

        assertThat(view.getId()).isEqualTo(1L);
        assertThat(view.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(view.getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 2, 10, 15, 30));
    }
}
