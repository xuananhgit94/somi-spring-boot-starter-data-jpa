package com.somi.jpa.handler;

import com.somi.jpa.annotations.QueryId;
import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.exceptions.NativeQueryException;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Metadata of one {@link QueryId} repository method, resolved and validated once at startup.
 *
 * @param queryId       id of the query in the XML files
 * @param modifying     whether the query is executed with {@code executeUpdate}
 * @param containerType declared return type, e.g. {@code List}, {@code Optional} or the element type itself
 * @param resultType    element type, e.g. {@code PersonDto} for {@code List<PersonDto>}
 */
public record QueryMethod(String queryId, boolean modifying, Class<?> containerType, Class<?> resultType) {

    private static final Set<Class<?>> MODIFYING_RETURN_TYPES =
            Set.of(void.class, Void.class, int.class, Integer.class, long.class, Long.class);

    public static QueryMethod from(Method method) {
        QueryId queryId = method.getAnnotation(QueryId.class);
        if (queryId == null) {
            throw NativeQueryException.of(ErrorMessage.MISSING_QUERYID, method.toGenericString());
        }
        Class<?> containerType = method.getReturnType();
        boolean modifying = switch (queryId.type()) {
            case MODIFIER -> true;
            case SELECT -> false;
            case AUTO -> containerType == void.class || containerType == Void.class;
        };
        if (modifying && !MODIFYING_RETURN_TYPES.contains(containerType)) {
            throw NativeQueryException.of(ErrorMessage.NOT_MODIFYING_QUERY, method.toGenericString(), containerType.getName());
        }
        return new QueryMethod(queryId.value(), modifying, containerType, resolveResultType(method));
    }

    public boolean returnsCollection() {
        return Collection.class.isAssignableFrom(containerType);
    }

    public boolean returnsOptional() {
        return containerType == Optional.class;
    }

    private static Class<?> resolveResultType(Method method) {
        ResolvableType returnType = ResolvableType.forMethodReturnType(method);
        Class<?> resolved = returnType.resolve();
        if (resolved != null && (Collection.class.isAssignableFrom(resolved) || resolved == Optional.class)) {
            resolved = returnType.getGeneric(0).resolve();
        }
        if (resolved == null) {
            throw NativeQueryException.of(ErrorMessage.CANNOT_RESOLVE_RETURN_TYPE, method.toGenericString());
        }
        return resolved;
    }
}
