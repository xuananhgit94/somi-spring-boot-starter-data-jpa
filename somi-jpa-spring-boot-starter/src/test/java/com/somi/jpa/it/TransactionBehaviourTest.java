package com.somi.jpa.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TransactionBehaviourTest {

    @Autowired
    PersonQueries queries;

    @Test
    void modifyingQueriesRunWithoutACallerTransaction() {
        try {
            assertThat(queries.insert(100L, "No Tx")).isEqualTo(1);
            assertThat(queries.findById(100L)).isPresent();
        } finally {
            queries.delete(100L);
        }
    }

    @Test
    void constraintViolationsAreTranslatedToDataAccessExceptions() {
        assertThatThrownBy(() -> queries.insert(1L, "Duplicate"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void entitiesCanBeReadWithoutACallerTransaction() {
        assertThat(queries.findAllEntities()).hasSize(2);
    }
}
