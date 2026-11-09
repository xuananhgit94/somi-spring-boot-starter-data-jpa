package com.somi.jpa.it;

import com.somi.jpa.annotations.QueryId;

import java.util.List;

public interface BaseQueries<T> {

    @QueryId("person.findAll")
    List<T> findAllGeneric();

    String describe();
}
