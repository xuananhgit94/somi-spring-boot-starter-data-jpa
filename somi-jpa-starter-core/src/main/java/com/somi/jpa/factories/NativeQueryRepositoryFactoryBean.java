package com.somi.jpa.factories;
import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.exceptions.DefaultNativeQueryException;
import com.somi.jpa.executors.QueryByIdExecutor;
import com.somi.jpa.handler.NativeQueryRepositoryHandler;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

public class NativeQueryRepositoryFactoryBean<T> implements FactoryBean<T> {
    private final Class<T> repositoryInterface;
    private QueryByIdExecutor queryByIdExecutor;
    public NativeQueryRepositoryFactoryBean(Class<T> repositoryInterface) {
        this.repositoryInterface = repositoryInterface;
    }
    @Autowired
    public void setQueryByIdExecutor(QueryByIdExecutor queryByIdExecutor) {
        this.queryByIdExecutor = queryByIdExecutor;
    }
    @Override
    public T getObject() {
        if (this.queryByIdExecutor == null) {
            DefaultNativeQueryException.fail(ErrorMessage.QUERYBYIDEXECUTOR_WAS_NOT_INJECTED);
        }
        NativeQueryRepositoryHandler handler = new NativeQueryRepositoryHandler(repositoryInterface, queryByIdExecutor);
        return handler.createProxy();
    }

    @Override
    public Class<?> getObjectType() {
        return repositoryInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
