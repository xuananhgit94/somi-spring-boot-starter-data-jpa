package com.somi.jpa.it;

import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.exceptions.NativeQueryException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"somi.jpa.max-rows=1", "somi.jpa.query-timeout=5s"})
class MaxRowsPropertyTest {

    @Autowired
    PersonQueries queries;

    @Test
    void collectionQueriesFailInsteadOfLoadingTooManyRows() {
        assertThatThrownBy(queries::findAllRecords)
                .isInstanceOfSatisfying(NativeQueryException.class,
                        e -> assertThat(e.getError()).isEqualTo(ErrorMessage.MAX_ROWS_EXCEEDED));
        assertThat(queries.findBornBefore(java.sql.Date.valueOf("1991-01-01"))).hasSize(1);
    }
}
