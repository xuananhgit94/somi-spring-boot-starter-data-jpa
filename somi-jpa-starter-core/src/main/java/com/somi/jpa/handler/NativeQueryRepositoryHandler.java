package com.somi.jpa.handler;

import com.somi.jpa.annotations.QueryId;
import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.exceptions.NativeQueryException;
import com.somi.jpa.executors.QueryByIdExecutor;
import org.jspecify.annotations.Nullable;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public final class NativeQueryRepositoryHandler implements InvocationHandler {
    private final Class<?> repositoryInterface;
    private final QueryByIdExecutor queryByIdExecutor;
    private final Map<Method, QueryMethod> queryMethods = new HashMap<>();

    public NativeQueryRepositoryHandler(Class<?> repositoryInterface, QueryByIdExecutor queryByIdExecutor) {
        this.repositoryInterface = repositoryInterface;
        this.queryByIdExecutor = queryByIdExecutor;
        for (Method method : repositoryInterface.getMethods()) {
            if (method.isAnnotationPresent(QueryId.class) && !method.isDefault() && !Modifier.isStatic(method.getModifiers())) {
                QueryMethod queryMethod = QueryMethod.from(method, repositoryInterface);
                queryByIdExecutor.validate(queryMethod);
                queryMethods.put(method, queryMethod);
            }
        }
    }

    @Override
    public @Nullable Object invoke(Object proxy, Method method, @Nullable Object @Nullable [] args) throws Throwable {
        if (ReflectionUtils.isEqualsMethod(method)) {
            return args != null && proxy == args[0];
        }
        if (ReflectionUtils.isHashCodeMethod(method)) {
            return System.identityHashCode(proxy);
        }
        if (ReflectionUtils.isToStringMethod(method)) {
            return "NativeQueryRepository[" + repositoryInterface.getName() + "]";
        }
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }
        if (method.isDefault()) {
            return InvocationHandler.invokeDefault(proxy, method, args);
        }
        QueryMethod queryMethod = queryMethods.get(method);
        if (queryMethod == null) {
            throw NativeQueryException.of(ErrorMessage.MISSING_QUERYID, repositoryInterface.getSimpleName() + "." + method.getName());
        }
        return queryByIdExecutor.execute(queryMethod, args);
    }

    public <T> T createProxy(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(repositoryInterface.getClassLoader(), new Class<?>[]{repositoryInterface}, this));
    }
}
