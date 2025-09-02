package com.koach.jpa.handler;

import com.koach.jpa.anotations.QueryId;
import com.koach.jpa.enums.ErrorMessage;
import com.koach.jpa.exceptions.DefaultNativeQueryException;
import com.koach.jpa.executors.QueryByIdExecutor;
import org.springframework.core.ResolvableType;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Optional;

public record NativeQueryRepositoryHandler(Class<?> repoInterface,
                                           QueryByIdExecutor queryByIdExecutor) implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }
        QueryId queryId = method.getAnnotation(QueryId.class);
        if (queryId == null) {
            DefaultNativeQueryException.fail(ErrorMessage.MISSING_QUERYID, method.getName());
        }
        Class<?> containerType = method.getReturnType();
        Class<?> actualType = resolveActualReturnType(method);
        return queryByIdExecutor.execute(queryId.value(), args, containerType, actualType, queryId.type());
    }

    private Class<?> resolveActualReturnType(Method method) {
        ResolvableType rt = ResolvableType.forMethodReturnType(method);
        Class<?> resolved = rt.resolve();
        if (resolved == null) {
            DefaultNativeQueryException.fail(ErrorMessage.CANNOT_RESOLVE_RETURN_TYPE, method.getName());
        }
        if (Collection.class.isAssignableFrom(resolved) || Optional.class.isAssignableFrom(resolved)) {
            Class<?> generic = rt.getGeneric(0).resolve();
            if (generic == null) {
                DefaultNativeQueryException.fail(ErrorMessage.CANNOT_RESOLVE_RETURN_TYPE, method.getName());
            }
            return generic;
        }
        return resolved;
    }

    @SuppressWarnings("unchecked")
    public <T> T createProxy() {
        return (T) Proxy.newProxyInstance(repoInterface.getClassLoader(), new Class[]{repoInterface}, this);
    }
}
