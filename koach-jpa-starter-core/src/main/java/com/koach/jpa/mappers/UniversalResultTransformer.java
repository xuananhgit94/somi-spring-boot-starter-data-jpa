package com.koach.jpa.mappers;
import com.koach.jpa.enums.ErrorMessage;
import com.koach.jpa.exceptions.DefaultNativeQueryException;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;

import java.util.*;

public record UniversalResultTransformer<T>(Class<T> targetClass,
                                            boolean isInterface) implements TupleTransformer<T>, ResultListTransformer<T> {
    @Override
    public List<T> transformList(List<T> list) {
        return list;
    }

    @Override
    public T transformTuple(Object[] tuple, String[] aliases) {
        ObjectMappingStrategy<T> strategy = MappingStrategyFactory.getStrategy(targetClass, isInterface);
        try {
            return strategy.map(tuple, aliases, targetClass);
        } catch (Exception e) {
            throw DefaultNativeQueryException.of(e, ErrorMessage.FAILED_TO_MAPPER, targetClass.getSimpleName());
        }
    }
}
