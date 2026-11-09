package com.somi.jpa.handler;

import com.somi.jpa.annotations.QueryId;
import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.enums.QueryType;
import com.somi.jpa.exceptions.NativeQueryException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueryMethodTest {

    interface Repo {
        @QueryId("list")
        List<String> list();

        @QueryId("optional")
        Optional<Long> optional();

        @QueryId("delete")
        void delete();

        @QueryId(value = "update", type = QueryType.MODIFIER)
        long update();

        @QueryId(value = "badUpdate", type = QueryType.MODIFIER)
        List<String> badUpdate();

        String noAnnotation();
    }

    @Test
    void resolvesElementTypesAndModifyingFlag() throws Exception {
        assertThat(QueryMethod.from(Repo.class.getMethod("list")))
                .isEqualTo(new QueryMethod("list", false, List.class, String.class));
        assertThat(QueryMethod.from(Repo.class.getMethod("optional")))
                .isEqualTo(new QueryMethod("optional", false, Optional.class, Long.class));
        assertThat(QueryMethod.from(Repo.class.getMethod("delete")).modifying()).isTrue();
        assertThat(QueryMethod.from(Repo.class.getMethod("update")).modifying()).isTrue();
    }

    @Test
    void rejectsInvalidMethodsUpFront() {
        assertThatThrownBy(() -> QueryMethod.from(Repo.class.getMethod("badUpdate")))
                .isInstanceOfSatisfying(NativeQueryException.class,
                        e -> assertThat(e.getError()).isEqualTo(ErrorMessage.NOT_MODIFYING_QUERY));
        assertThatThrownBy(() -> QueryMethod.from(Repo.class.getMethod("noAnnotation")))
                .isInstanceOfSatisfying(NativeQueryException.class,
                        e -> assertThat(e.getError()).isEqualTo(ErrorMessage.MISSING_QUERYID));
    }
}
