package com.koach.jpa.mappers;

public interface ObjectMappingStrategy<T> {
    T map(Object[] tuple, String[] aliases, Class<T> targetClass) throws Exception;
}
