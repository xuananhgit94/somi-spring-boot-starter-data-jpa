package com.somi.jpa.mappers;

import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.exceptions.NativeQueryException;
import com.somi.jpa.mappers.converters.ValueConversionService;
import org.hibernate.query.TupleTransformer;
import org.jspecify.annotations.Nullable;

public record UniversalResultTransformer<T>(Class<T> targetClass,
                                            ObjectMappingStrategy<T> strategy) implements TupleTransformer<T> {

    public UniversalResultTransformer(Class<T> targetClass, ValueConversionService conversionService) {
        this(targetClass, ObjectMappingStrategy.of(targetClass, conversionService));
    }

    @Override
    public T transformTuple(@Nullable Object[] tuple, String[] aliases) {
        try {
            return strategy.map(tuple, aliases);
        } catch (NativeQueryException e) {
            throw e;
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw NativeQueryException.wrap(e, ErrorMessage.FAILED_TO_MAPPER, targetClass.getSimpleName());
        }
    }
}
