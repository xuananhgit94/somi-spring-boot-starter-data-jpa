package com.somi.jpa.it;

import com.somi.jpa.exceptions.NativeQueryException;
import com.somi.jpa.providers.QueryByIdProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "somi.jpa.query-locations=classpath*:/no-such-dir/*.xml")
class QueryLocationsPropertyTest {

    @Autowired
    QueryByIdProvider queryByIdProvider;

    @Test
    void queryLocationsPropertyReplacesTheDefault() {
        assertThatThrownBy(() -> queryByIdProvider.getQuery("person.findAll"))
                .isInstanceOf(NativeQueryException.class);
    }
}
