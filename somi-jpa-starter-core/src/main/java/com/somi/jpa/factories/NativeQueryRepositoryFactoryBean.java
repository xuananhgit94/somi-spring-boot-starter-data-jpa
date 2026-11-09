package com.somi.jpa.factories;

import com.somi.jpa.executors.QueryByIdExecutor;
import com.somi.jpa.handler.NativeQueryRepositoryHandler;
import org.springframework.beans.factory.FactoryBean;

/**
 * Creates the proxy for one repository interface. Registered by
 * {@link com.somi.jpa.registers.NativeQueryRepositoryRegistrar} with constructor autowiring.
 */
public class NativeQueryRepositoryFactoryBean<T> implements FactoryBean<T> {
    private final Class<T> repositoryInterface;
    private final T repository;

    public NativeQueryRepositoryFactoryBean(Class<T> repositoryInterface, QueryByIdExecutor queryByIdExecutor) {
        this.repositoryInterface = repositoryInterface;
        this.repository = new NativeQueryRepositoryHandler(repositoryInterface, queryByIdExecutor)
                .createProxy(repositoryInterface);
    }

    @Override
    public T getObject() {
        return repository;
    }

    @Override
    public Class<T> getObjectType() {
        return repositoryInterface;
    }
}
