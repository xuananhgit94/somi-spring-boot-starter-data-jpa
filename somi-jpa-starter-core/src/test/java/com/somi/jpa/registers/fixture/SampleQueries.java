package com.somi.jpa.registers.fixture;

import com.somi.jpa.annotations.QueryId;

import java.util.List;

public interface SampleQueries {
    @QueryId("sample.all")
    List<String> findAll();
}
