package com.somi.jpa.enums;

import lombok.Getter;

@Getter
public enum ErrorMessage {
    LOAD_QUERY_ERROR("error.load.query", "No result for native query %s"),
    DUPLICATE_QUERY_ID("error.query.duplicate", "Query Id %s duplicated at file %s"),
    FAIL_PARSE_QUERY("error.query.parse", "Fail to parse query file %s"),
    QUERY_ID_NOT_FOUND("error.query.not.found", "Query id %s not found"),
    NOT_MODIFYING_QUERY("error.query.not.modifying", "Modifying query with QueryId.type MODIFIER must have void or int return type, but found %s"),
    FAILED_TO_UNWRAP_QUERY("error.query.fail.unwrap", "Failed to unwrap EntityManager Query to Hibernate NativeQuery"),
    FAILED_TO_REGISTER("error.query.fail.register", "Failed to register native query repository: %s"),
    MUST_BE_INTERFACE("error.query.must.be.interface", "Native query repository target must be interface: %s"),
    QUERYBYIDEXECUTOR_WAS_NOT_INJECTED("error.query.by.id.executor.not.found", "QueryByIdExecutor was not injected into NativeQueryRepositoryFactoryBean"),
    MISSING_QUERYID("error.query.missing.queryid", "Missing @QueryId on method: %s"),
    CANNOT_RESOLVE_RETURN_TYPE("error.cannot.resolve.return.type", "Cannot resolve return type for method: %s"),
    FAILED_TO_MAPPER("error.mapper.failed", "Failed to map row to DTO/Record: %s"),
    SQL_TO_ARRAY_FAIL("error.convert.array.fail", "Failed to convert SQL Array"),
    STRING_TO_CHARACTER_FAIL("error.convert.character.fail", "Cannot convert String %s to Character. String must be a single character"),
    STRING_TO_JSON_FAIL("error.convert.json.fail", "Failed to parse JSON string to %s. JSON: %s"),
    STRING_TO_ENUM_FAIL("error.convert.enum.fail", "Failed to convert String %s to Enum type %s"),
    STRING_TO_UUID_FAIL("error.convert.uuid.fail", "Failed to convert String %s to UUID"),
    CONVERT_VALUE_FAIL("error.convert.value.fail", "Failed to convert value %s to %s");


    private final String code;
    private final String defaultTemplate;

    ErrorMessage(String code, String defaultTemplate) {
        this.code = code;
        this.defaultTemplate = defaultTemplate;
    }

    public String format(Object... args) {
        return String.format(defaultTemplate, args);
    }
}
