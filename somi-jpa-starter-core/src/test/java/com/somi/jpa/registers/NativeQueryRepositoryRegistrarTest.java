package com.somi.jpa.registers;

import com.somi.jpa.annotations.EnableNativeQueries;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NativeQueryRepositoryRegistrarTest {

    @Configuration
    @EnableNativeQueries(basePackages = "com.somi.jpa.registers.fixture")
    static class ConflictingConfig {
        @Bean
        Object sampleQueries() {
            return new Object();
        }
    }

    @Test
    void failsWhenAnotherBeanAlreadyUsesTheRepositoryName() {
        assertThatThrownBy(() -> new AnnotationConfigApplicationContext(ConflictingConfig.class).close())
                .hasStackTraceContaining("Bean name sampleQueries for repository com.somi.jpa.registers.fixture.SampleQueries");
    }
}
