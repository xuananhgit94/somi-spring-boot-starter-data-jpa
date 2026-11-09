package com.somi.jpa.it;

import com.somi.jpa.annotations.QueryId;
import com.somi.jpa.enums.QueryType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public interface PersonQueries extends BaseQueries<PersonQueries.PersonRecord> {

    enum Status { ACTIVE, INACTIVE }

    record PersonRecord(Long id, String fullName, Status status, BigDecimal salary,
                        boolean active, LocalDate birthDate, LocalDateTime createdAt) {}

    interface PersonView {
        Long getId();
        String getFullName();
        Status getStatus();
        LocalDateTime getCreatedAt();
    }

    class PersonDto {
        private Long id;
        private String fullName;
        private Status status;
        private BigDecimal salary;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public Status getStatus() { return status; }
        public void setStatus(Status status) { this.status = status; }
        public BigDecimal getSalary() { return salary; }
        public void setSalary(BigDecimal salary) { this.salary = salary; }
    }

    @QueryId("person.findAll")
    List<PersonRecord> findAllRecords();

    @QueryId("person.findAll")
    List<PersonDto> findAllDtos();

    @QueryId("person.findAll")
    List<PersonView> findAllViews();

    @QueryId("person.findAll")
    PersonRecord findAnyOne();

    @QueryId("person.findById")
    Optional<PersonRecord> findById(Long id);

    @QueryId("person.findById")
    PersonDto findDtoById(Long id);

    @QueryId("person.findById")
    Optional<Person> findEntityById(Long id);

    @QueryId("person.findAll")
    List<Person> findAllEntities();

    @QueryId("person.findByStatus")
    List<PersonRecord> findByStatus(Status status);

    @QueryId("person.findBornBefore")
    List<Long> findBornBefore(java.sql.Date date);

    @QueryId("person.findIds")
    Set<Long> findAllIds();

    @QueryId("person.findIds")
    TreeSet<Long> findIdsSorted();

    @QueryId("person.findIds")
    ArrayList<Long> findIdsList();

    default Optional<String> findName(Long id) {
        return findById(id).map(PersonRecord::fullName);
    }

    @QueryId("person.countActive")
    Long countActive();

    @QueryId(value = "person.deactivate", type = QueryType.MODIFIER)
    int deactivate(Long id);

    @QueryId("person.deactivate")
    void deactivateVoid(Long id);

    @QueryId(value = "person.rename", type = QueryType.MODIFIER)
    int rename(Long id, String fullName);

    @QueryId(value = "person.rename", type = QueryType.MODIFIER, clearAutomatically = true)
    int renameAndClear(Long id, String fullName);

    @QueryId(value = "person.insert", type = QueryType.MODIFIER)
    int insert(Long id, String fullName);

    @QueryId(value = "person.delete", type = QueryType.MODIFIER)
    int delete(Long id);
}
