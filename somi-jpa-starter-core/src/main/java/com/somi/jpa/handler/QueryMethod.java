package com.somi.jpa.handler;

import com.somi.jpa.annotations.QueryId;
import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.exceptions.NativeQueryException;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public record QueryMethod(String name,
                          String queryId,
                          boolean modifying,
                          boolean flushAutomatically,
                          boolean clearAutomatically,
                          Class<?> containerType,
                          Class<?> resultType,
                          List<Class<?>> parameterTypes) {

    private static final Set<Class<?>> MODIFYING_RETURN_TYPES =
            Set.of(void.class, Void.class, int.class, Integer.class, long.class, Long.class);

    public static QueryMethod from(Method method, Class<?> repositoryInterface) {
        String name = repositoryInterface.getSimpleName() + "." + method.getName();
        QueryId queryId = method.getAnnotation(QueryId.class);
        if (queryId == null) {
            throw NativeQueryException.of(ErrorMessage.MISSING_QUERYID, name);
        }
        Class<?> containerType = method.getReturnType();
        boolean modifying = switch (queryId.type()) {
            case MODIFIER -> true;
            case SELECT -> false;
            case AUTO -> containerType == void.class || containerType == Void.class;
        };
        if (modifying && !MODIFYING_RETURN_TYPES.contains(containerType)) {
            throw NativeQueryException.of(ErrorMessage.NOT_MODIFYING_QUERY, name, containerType.getName());
        }
        return new QueryMethod(name, queryId.value(), modifying, queryId.flushAutomatically(),
                queryId.clearAutomatically(), containerType, resolveResultType(method, repositoryInterface, name),
                List.of(method.getParameterTypes()));
    }

    public boolean returnsCollection() {
        return Collection.class.isAssignableFrom(containerType);
    }

    public boolean returnsOptional() {
        return containerType == Optional.class;
    }

    private static Class<?> resolveResultType(Method method, Class<?> repositoryInterface, String name) {
        ResolvableType returnType = ResolvableType.forMethodReturnType(method, repositoryInterface);
        Class<?> resolved = returnType.resolve();
        if (resolved != null && (Collection.class.isAssignableFrom(resolved) || resolved == Optional.class)) {
            resolved = returnType.getGeneric(0).resolve();
        }
        if (resolved == null || resolved == Object.class) {
            throw NativeQueryException.of(ErrorMessage.CANNOT_RESOLVE_RETURN_TYPE, name);
        }
        return resolved;
    }
}
