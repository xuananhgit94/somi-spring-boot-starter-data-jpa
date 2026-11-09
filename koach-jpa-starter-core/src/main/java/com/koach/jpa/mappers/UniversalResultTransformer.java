package com.koach.jpa.mappers;
import com.koach.jpa.enums.ErrorMessage;
import com.koach.jpa.exceptions.DefaultNativeQueryException;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;

import java.util.*;

public record UniversalResultTransformer<T>(Class<T> targetClass,
                                            boolean isInterface,
                                            ObjectMappingStrategy<T> strategy) implements TupleTransformer<T>, ResultListTransformer<T> {

    // Resolve the strategy once so its reflection caches live for the whole result set
    public UniversalResultTransformer(Class<T> targetClass, boolean isInterface) {
        this(targetClass, isInterface, MappingStrategyFactory.getStrategy(targetClass, isInterface));
    }

    @Override
    public List<T> transformList(List<T> list) {
        return list;
    }

    @Override
    public T transformTuple(Object[] tuple, String[] aliases) {
        try {
            return strategy.map(tuple, aliases, targetClass);
        } catch (Exception e) {
            throw DefaultNativeQueryException.of(e, ErrorMessage.FAILED_TO_MAPPER, targetClass.getSimpleName());
        }
    }
}
