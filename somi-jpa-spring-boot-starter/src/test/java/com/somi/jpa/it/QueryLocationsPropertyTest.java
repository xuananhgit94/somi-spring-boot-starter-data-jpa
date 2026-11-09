package com.somi.jpa.it;

import com.somi.jpa.providers.QueryByIdProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "somi.jpa.query-locations=classpath*:/queries/*.xml,classpath*:/alt-queries/*.xml")
class QueryLocationsPropertyTest {

    @Autowired
    QueryByIdProvider queryByIdProvider;

    @Test
    void queryLocationsPropertyReplacesTheDefault() {
        assertThat(queryByIdProvider.contains("alt.marker")).isTrue();
    }

    @Test
    void startupFailsWhenARepositoryUsesAnUnknownQueryId() {
        SpringApplicationBuilder app = new SpringApplicationBuilder(TestApplication.class)
                .properties("somi.jpa.query-locations=classpath*:/alt-queries/*.xml", "spring.main.banner-mode=off");

        assertThatThrownBy(app::run).hasStackTraceContaining("used by PersonQueries.");
    }
}
