package com.somi.jpa.providers;

import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.exceptions.NativeQueryException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueryByIdProviderTest {

    @Test
    void loadsQueriesUnderTheFileNameNamespaceAndSkipsInvalidNodes() {
        QueryByIdProvider provider = new QueryByIdProvider(List.of("classpath:/queries/ok/*.xml"));

        assertThat(provider.getQuery("a.FIND")).isEqualTo("SELECT 1");
        assertThat(provider.contains("FIND")).isFalse();
        assertThatThrownBy(() -> provider.getQuery("a.MISSING"))
                .isInstanceOfSatisfying(NativeQueryException.class,
                        e -> assertThat(e.getError()).isEqualTo(ErrorMessage.QUERY_ID_NOT_FOUND));
    }

    @Test
    void sameIdMayBeUsedInDifferentFiles() {
        QueryByIdProvider provider = new QueryByIdProvider(List.of("classpath:/queries/multi/*.xml"));

        assertThat(provider.getQuery("user.FIND_BY_ID")).contains("FROM users");
        assertThat(provider.getQuery("sales.FIND_BY_ID")).contains("FROM orders");
    }

    @Test
    void rejectsDuplicateIdsInOneFile() {
        assertThatThrownBy(() -> new QueryByIdProvider(List.of("classpath:/queries/dup/*.xml")))
                .isInstanceOfSatisfying(NativeQueryException.class,
                        e -> assertThat(e.getError()).isEqualTo(ErrorMessage.DUPLICATE_QUERY_ID));
    }

    @Test
    void rejectsDuplicateIdsAcrossFilesSharingANamespace() {
        assertThatThrownBy(() -> new QueryByIdProvider(List.of("classpath:/queries/shared/*.xml")))
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
