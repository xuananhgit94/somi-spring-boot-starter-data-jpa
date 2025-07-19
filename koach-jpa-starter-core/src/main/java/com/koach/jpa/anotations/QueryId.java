package com.koach.jpa.anotations;

import com.koach.jpa.enums.QueryType;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface QueryId {
    String value();
    QueryType type() default QueryType.AUTO;
}
