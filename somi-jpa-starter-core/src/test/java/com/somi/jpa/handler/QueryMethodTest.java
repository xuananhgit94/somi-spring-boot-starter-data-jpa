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
        List<String> list(Long id);

        @QueryId("optional")
        Optional<Long> optional();

        @QueryId("delete")
        void delete();

        @QueryId(value = "update", type = QueryType.MODIFIER, flushAutomatically = true, clearAutomatically = true)
        long update();

        @QueryId(value = "badUpdate", type = QueryType.MODIFIER)
        List<String> badUpdate();

        String noAnnotation();
    }

    interface Base<T> {
        @QueryId("base.all")
        List<T> findAll();
    }

    record Row(Long id) {}

    interface RowRepo extends Base<Row> {}

    @Test
    void resolvesElementTypesFlagsAndParameters() throws Exception {
        assertThat(QueryMethod.from(Repo.class.getMethod("list", Long.class), Repo.class)).isEqualTo(
                new QueryMethod("Repo.list", "list", false, false, false, List.class, String.class, List.of(Long.class)));
        assertThat(QueryMethod.from(Repo.class.getMethod("optional"), Repo.class).resultType()).isEqualTo(Long.class);
        assertThat(QueryMethod.from(Repo.class.getMethod("delete"), Repo.class).modifying()).isTrue();
        QueryMethod update = QueryMethod.from(Repo.class.getMethod("update"), Repo.class);
        assertThat(update.modifying()).isTrue();
        assertThat(update.flushAutomatically()).isTrue();
        assertThat(update.clearAutomatically()).isTrue();
    }

    @Test
    void resolvesGenericsDeclaredOnABaseInterface() throws Exception {
        QueryMethod method = QueryMethod.from(RowRepo.class.getMethod("findAll"), RowRepo.class);

        assertThat(method.resultType()).isEqualTo(Row.class);
    }

    @Test
    void rejectsInvalidMethodsUpFront() {
        assertThatThrownBy(() -> QueryMethod.from(Repo.class.getMethod("badUpdate"), Repo.class))
                .isInstanceOfSatisfying(NativeQueryException.class,
                        e -> assertThat(e.getError()).isEqualTo(ErrorMessage.NOT_MODIFYING_QUERY));
        assertThatThrownBy(() -> QueryMethod.from(Repo.class.getMethod("noAnnotation"), Repo.class))
                .isInstanceOfSatisfying(NativeQueryException.class,
                        e -> assertThat(e.getError()).isEqualTo(ErrorMessage.MISSING_QUERYID));
        assertThatThrownBy(() -> QueryMethod.from(Base.class.getMethod("findAll"), Base.class))
                .isInstanceOfSatisfying(NativeQueryException.class,
                        e -> assertThat(e.getError()).isEqualTo(ErrorMessage.CANNOT_RESOLVE_RETURN_TYPE));
    }
}
