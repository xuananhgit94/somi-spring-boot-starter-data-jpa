package com.somi.jpa.providers;

import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.exceptions.NativeQueryException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueryByIdProviderTest {

    @Test
    void loadsQueriesAndSkipsInvalidNodes() {
        QueryByIdProvider provider = new QueryByIdProvider(List.of("classpath:/queries/ok/*.xml"));

        assertThat(provider.getQuery("a.find")).isEqualTo("SELECT 1");
        assertThatThrownBy(() -> provider.getQuery("missing"))
                .isInstanceOfSatisfying(NativeQueryException.class,
                        e -> assertThat(e.getError()).isEqualTo(ErrorMessage.QUERY_ID_NOT_FOUND));
    }

    @Test
    void rejectsDuplicateIds() {
        assertThatThrownBy(() -> new QueryByIdProvider(List.of("classpath:/queries/dup/*.xml")))
                .isInstanceOfSatisfying(NativeQueryException.class,
                        e -> assertThat(e.getError()).isEqualTo(ErrorMessage.DUPLICATE_QUERY_ID));
    }

    @Test
    void rejectsDoctypeDeclarations() {
        assertThatThrownBy(() -> new QueryByIdProvider(List.of("classpath:/queries/xxe.xml")))
                .isInstanceOfSatisfying(NativeQueryException.class,
                        e -> assertThat(e.getError()).isEqualTo(ErrorMessage.FAIL_PARSE_QUERY));
    }
}
