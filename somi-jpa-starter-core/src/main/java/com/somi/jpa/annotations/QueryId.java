package com.somi.jpa.annotations;

import com.somi.jpa.enums.QueryType;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface QueryId {
    String value();

    String namespace() default "";

    QueryType type() default QueryType.AUTO;

    boolean flushAutomatically() default false;

    boolean clearAutomatically() default false;
}
