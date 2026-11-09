package com.somi.jpa.handler;

import com.somi.jpa.executors.QueryByIdExecutor;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Backs a repository interface: every abstract method runs the native query named by its {@code @QueryId},
 * default methods run their own body.
 */
public final class NativeQueryRepositoryHandler implements InvocationHandler {
    private final Class<?> repositoryInterface;
    private final QueryByIdExecutor queryByIdExecutor;
    private final Map<Method, QueryMethod> queryMethods = new HashMap<>();

    public NativeQueryRepositoryHandler(Class<?> repositoryInterface, QueryByIdExecutor queryByIdExecutor) {
        this.repositoryInterface = repositoryInterface;
        this.queryByIdExecutor = queryByIdExecutor;
        // Validate every method up front so a broken repository fails at startup, not on first call
        for (Method method : repositoryInterface.getMethods()) {
            if (!method.isDefault() && !Modifier.isStatic(method.getModifiers())) {
                queryMethods.put(method, QueryMethod.from(method));
            }
        }
    }

    @Override
    public @Nullable Object invoke(Object proxy, Method method, @Nullable Object @Nullable [] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return switch (method.getName()) {
                case "equals" -> args != null && proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "NativeQueryRepository[" + repositoryInterface.getName() + "]";
                default -> method.invoke(this, args);
            };
        }
        if (method.isDefault()) {
            return InvocationHandler.invokeDefault(proxy, method, args);
        }
        QueryMethod queryMethod = queryMethods.get(method);
        if (queryMethod == null) {
            throw new IllegalStateException("Not a query method of " + repositoryInterface.getName() + ": " + method);
        }
        return queryByIdExecutor.execute(queryMethod, args);
    }

    public <T> T createProxy(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(repositoryInterface.getClassLoader(), new Class<?>[]{repositoryInterface}, this));
    }
}
