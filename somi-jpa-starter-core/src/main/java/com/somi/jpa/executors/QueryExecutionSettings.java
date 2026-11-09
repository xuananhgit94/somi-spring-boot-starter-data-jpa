package com.somi.jpa.executors;

import org.jspecify.annotations.Nullable;

import java.time.Duration;

public record QueryExecutionSettings(boolean logParameterValues,
                                     @Nullable Duration queryTimeout,
                                     @Nullable Integer maxRows) {

    public static QueryExecutionSettings defaults() {
        return new QueryExecutionSettings(false, null, null);
    }
}
