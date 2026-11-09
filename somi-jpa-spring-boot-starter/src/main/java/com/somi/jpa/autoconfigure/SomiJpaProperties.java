package com.somi.jpa.autoconfigure;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties("somi.jpa")
public record SomiJpaProperties(@DefaultValue("classpath*:/queries/**/*.xml") List<String> queryLocations,
                                @DefaultValue("false") boolean logParameterValues,
                                @Nullable Duration queryTimeout,
                                @Nullable Integer maxRows) {
}
