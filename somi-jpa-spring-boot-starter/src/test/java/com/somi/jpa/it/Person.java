package com.somi.jpa.it;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "person")
public class Person {
    @Id
    private Long id;
    @Column(name = "full_name")
    private String fullName;
    private String status;
    private BigDecimal salary;
    private boolean active;
    @Column(name = "birth_date")
    private LocalDate birthDate;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getStatus() { return status; }
    public BigDecimal getSalary() { return salary; }
    public boolean isActive() { return active; }
    public LocalDate getBirthDate() { return birthDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
