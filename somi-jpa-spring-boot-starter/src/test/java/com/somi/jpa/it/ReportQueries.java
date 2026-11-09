package com.somi.jpa.it;

import com.somi.jpa.annotations.QueryId;
import com.somi.jpa.annotations.QueryNamespace;

import java.util.Optional;

@QueryNamespace("report")
public interface ReportQueries {

    @QueryId("FIND_BY_ID")
    Optional<String> findDisplayName(Long id);

    @QueryId(value = "FIND_BY_ID", namespace = "person")
    Optional<PersonQueries.PersonRecord> findPerson(Long id);
}
