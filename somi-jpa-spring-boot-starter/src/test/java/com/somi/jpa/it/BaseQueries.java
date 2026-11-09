package com.somi.jpa.it;

import com.somi.jpa.annotations.QueryId;

import java.util.List;

public interface BaseQueries<T> {

    @QueryId("FIND_ALL")
    List<T> findAllGeneric();

    String describe();
}
