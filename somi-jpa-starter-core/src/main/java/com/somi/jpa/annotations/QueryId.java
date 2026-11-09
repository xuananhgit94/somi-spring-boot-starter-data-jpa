package com.somi.jpa.annotations;

import com.somi.jpa.enums.QueryType;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface QueryId {
    String value();
    QueryType type() default QueryType.AUTO;
}
