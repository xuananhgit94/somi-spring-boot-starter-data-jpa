package com.somi.jpa.it;

import com.somi.jpa.annotations.QueryId;
import com.somi.jpa.annotations.QueryNamespace;
import com.somi.jpa.enums.QueryType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@QueryNamespace("person")
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

    @QueryId("FIND_ALL")
    List<PersonRecord> findAllRecords();

    @QueryId("FIND_ALL")
    List<PersonDto> findAllDtos();

    @QueryId("FIND_ALL")
    List<PersonView> findAllViews();

    @QueryId("FIND_ALL")
    PersonRecord findAnyOne();

    @QueryId("FIND_BY_ID")
    Optional<PersonRecord> findById(Long id);

    @QueryId("FIND_BY_ID")
    PersonDto findDtoById(Long id);

    @QueryId("FIND_BY_ID")
    Optional<Person> findEntityById(Long id);

    @QueryId("FIND_ALL")
    List<Person> findAllEntities();

    @QueryId("FIND_BY_STATUS")
    List<PersonRecord> findByStatus(Status status);

    @QueryId("FIND_BORN_BEFORE")
    List<Long> findBornBefore(java.sql.Date date);

    @QueryId("FIND_IDS")
    Set<Long> findAllIds();

    @QueryId("FIND_IDS")
    TreeSet<Long> findIdsSorted();

    @QueryId("FIND_IDS")
    ArrayList<Long> findIdsList();

    default Optional<String> findName(Long id) {
        return findById(id).map(PersonRecord::fullName);
    }

    @QueryId("COUNT_ACTIVE")
    Long countActive();

    @QueryId(value = "DEACTIVATE", type = QueryType.MODIFIER)
    int deactivate(Long id);

    @QueryId("DEACTIVATE")
    void deactivateVoid(Long id);

    @QueryId(value = "RENAME", type = QueryType.MODIFIER)
    int rename(Long id, String fullName);

    @QueryId(value = "RENAME", type = QueryType.MODIFIER, clearAutomatically = true)
    int renameAndClear(Long id, String fullName);

    @QueryId(value = "INSERT", type = QueryType.MODIFIER)
    int insert(Long id, String fullName);

    @QueryId(value = "DELETE", type = QueryType.MODIFIER)
    int delete(Long id);
}
