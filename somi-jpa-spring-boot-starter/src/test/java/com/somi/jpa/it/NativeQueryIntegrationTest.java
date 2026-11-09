package com.somi.jpa.it;

import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.exceptions.NativeQueryException;
import com.somi.jpa.it.PersonQueries.PersonDto;
import com.somi.jpa.it.PersonQueries.PersonRecord;
import com.somi.jpa.it.PersonQueries.PersonView;
import com.somi.jpa.it.PersonQueries.Status;
import com.somi.jpa.providers.QueryByIdProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class NativeQueryIntegrationTest {

    @Autowired
    PersonQueries queries;

    @Autowired
    QueryByIdProvider queryByIdProvider;

    @Test
    void mapsRowsToRecords() {
        List<PersonRecord> people = queries.findAllRecords();

        assertThat(people).containsExactly(
                new PersonRecord(1L, "Nguyen Van A", Status.ACTIVE, new BigDecimal("1000.50"), true,
                        LocalDate.of(1990, 5, 1), LocalDateTime.of(2024, 1, 2, 10, 15, 30)),
                new PersonRecord(2L, "Tran Thi B", Status.ACTIVE, new BigDecimal("2000.00"), true,
                        LocalDate.of(1992, 7, 15), LocalDateTime.of(2024, 3, 4, 8, 0)));
    }

    @Test
    void mapsRowsToDtos() {
        List<PersonDto> people = queries.findAllDtos();

        assertThat(people).extracting(PersonDto::getFullName).containsExactly("Nguyen Van A", "Tran Thi B");
        assertThat(people.getFirst().getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(people.getFirst().getSalary()).isEqualByComparingTo("1000.50");
    }

    @Test
    void mapsRowsToInterfaceProjections() {
        List<PersonView> people = queries.findAllViews();

        assertThat(people).extracting(PersonView::getId).containsExactly(1L, 2L);
        assertThat(people.getFirst().getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 2, 10, 15, 30));
    }

    @Test
    void mapsRowsToEntities() {
        List<Person> people = queries.findAllEntities();

        assertThat(people).extracting(Person::getFullName).containsExactly("Nguyen Van A", "Tran Thi B");
    }

    @Test
    void singleResultsAndOptionals() {
        assertThat(queries.findById(1L)).get().extracting(PersonRecord::fullName).isEqualTo("Nguyen Van A");
        assertThat(queries.findById(99L)).isEmpty();
        assertThat(queries.findDtoById(99L)).isNull();
        assertThat(queries.countActive()).isEqualTo(2L);
    }

    @Test
    void modifyingQueries() {
        assertThat(queries.deactivate(1L)).isEqualTo(1);
        queries.deactivateVoid(2L);

        assertThat(queries.countActive()).isZero();
        assertThat(queries.findById(1L)).get().extracting(PersonRecord::status).isEqualTo(Status.INACTIVE);
    }

    @Test
    void setReturnTypesAndDefaultMethods() {
        assertThat(queries.findAllIds()).isInstanceOf(Set.class).containsExactly(1L, 2L);
        assertThat(queries.findName(2L)).contains("Tran Thi B");
        assertThat(queries).hasToString("NativeQueryRepository[" + PersonQueries.class.getName() + "]");
    }

    @Test
    void unknownQueryIdFails() {
        assertThatThrownBy(() -> queryByIdProvider.getQuery("missing.id"))
                .isInstanceOf(NativeQueryException.class)
                .hasMessageContaining("missing.id");
    }

    @Autowired
    ReportQueries reportQueries;

    @Test
    void sameQueryIdInDifferentFilesIsResolvedByNamespace() {
        assertThat(reportQueries.findDisplayName(1L)).contains("NGUYEN VAN A");
        assertThat(reportQueries.findPerson(1L)).get().extracting(PersonRecord::fullName).isEqualTo("Nguyen Van A");
        assertThat(queryByIdProvider.contains("person.FIND_BY_ID")).isTrue();
        assertThat(queryByIdProvider.contains("report.FIND_BY_ID")).isTrue();
    }

    @Test
    void concreteAndSortedCollectionTypes() {
        assertThat(queries.findIdsSorted()).isInstanceOf(java.util.TreeSet.class).containsExactly(1L, 2L);
        assertThat(queries.findIdsList()).isInstanceOf(java.util.ArrayList.class).containsExactly(1L, 2L);
    }

    @Test
    void singleResultMethodFailsWhenSeveralRowsMatch() {
        assertThatThrownBy(queries::findAnyOne).isInstanceOf(IncorrectResultSizeDataAccessException.class);
    }

    @Test
    void enumAndNullParametersAreBoundByName() {
        assertThat(queries.findByStatus(Status.ACTIVE)).hasSize(2);
        assertThat(queries.findByStatus(Status.INACTIVE)).isEmpty();
        assertThat(queries.findByStatus(null)).hasSize(2);
    }

    @Test
    void sqlDateParametersWorkWithDebugLogging() {
        assertThat(queries.findBornBefore(java.sql.Date.valueOf("1991-01-01"))).containsExactly(1L);
    }

    @Test
    void inheritedGenericMethodsMapAndUnannotatedMethodsFailOnlyWhenCalled() {
        assertThat(queries.findAllGeneric()).hasSize(2).first().isInstanceOf(PersonRecord.class);
        assertThatThrownBy(queries::describe)
                .isInstanceOfSatisfying(NativeQueryException.class,
                        e -> assertThat(e.getError()).isEqualTo(ErrorMessage.MISSING_QUERYID));
    }

    @Test
    void clearAutomaticallyRefreshesManagedEntities() {
        assertThat(queries.findEntityById(1L)).get().extracting(Person::getFullName).isEqualTo("Nguyen Van A");

        queries.renameAndClear(1L, "Renamed");

        assertThat(queries.findEntityById(1L)).get().extracting(Person::getFullName).isEqualTo("Renamed");
    }
}
