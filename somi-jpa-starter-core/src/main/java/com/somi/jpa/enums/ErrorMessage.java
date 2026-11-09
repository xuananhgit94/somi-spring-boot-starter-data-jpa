package com.somi.jpa.enums;

import java.text.MessageFormat;

/**
 * Error codes and English default templates ({@link MessageFormat} syntax).
 * Templates never include row values, only types, ids and file names, so messages are safe to log.
 */
public enum ErrorMessage {
    LOAD_QUERY_ERROR("somi.jpa.error.query.load", "Failed to load query files from {0}"),
    DUPLICATE_QUERY_ID("somi.jpa.error.query.duplicate", "Query id {0} duplicated in file {1}"),
    FAIL_PARSE_QUERY("somi.jpa.error.query.parse", "Failed to parse query file {0}"),
    QUERY_ID_NOT_FOUND("somi.jpa.error.query.not-found", "Query id {0} not found"),
    NOT_MODIFYING_QUERY("somi.jpa.error.query.not-modifying", "Modifying query must return void, int or long, but {0} returns {1}"),
    FAILED_TO_UNWRAP_QUERY("somi.jpa.error.query.unwrap", "Failed to unwrap JPA Query to Hibernate NativeQuery"),
    FAILED_TO_REGISTER("somi.jpa.error.repository.register", "Failed to register native query repository {0}"),
    MISSING_QUERYID("somi.jpa.error.repository.missing-query-id", "Missing @QueryId on method {0}"),
    CANNOT_RESOLVE_RETURN_TYPE("somi.jpa.error.repository.return-type", "Cannot resolve return type of method {0}"),
    FAILED_TO_MAPPER("somi.jpa.error.mapper.failed", "Failed to map row to {0}"),
    SQL_TO_ARRAY_FAIL("somi.jpa.error.convert.array", "Failed to read SQL array"),
    STRING_TO_CHARACTER_FAIL("somi.jpa.error.convert.character", "Cannot convert a string of length {0} to Character"),
    STRING_TO_JSON_FAIL("somi.jpa.error.convert.json", "Failed to parse JSON into {0}"),
    STRING_TO_ENUM_FAIL("somi.jpa.error.convert.enum", "Value is not a constant of enum {0}"),
    STRING_TO_UUID_FAIL("somi.jpa.error.convert.uuid", "Value is not a valid UUID"),
    CONVERT_VALUE_FAIL("somi.jpa.error.convert.value", "Failed to convert value of type {0} to {1}");

    private final String code;
    private final String defaultTemplate;

    ErrorMessage(String code, String defaultTemplate) {
        this.code = code;
        this.defaultTemplate = defaultTemplate;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultTemplate() {
        return defaultTemplate;
    }

    public String format(Object... args) {
        return MessageFormat.format(defaultTemplate, args);
    }
}
