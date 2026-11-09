package com.somi.jpa.mappers;

public class MappingStrategyFactory {
    public static <T> ObjectMappingStrategy<T> getStrategy(Class<T> targetClass, boolean isInterface) {
        if (isInterface) return new InterfaceProxyMappingStrategy<>();
        if (targetClass.isRecord()) return new RecordMappingStrategy<>();
        return new DtoMappingStrategy<>();
    }
}
