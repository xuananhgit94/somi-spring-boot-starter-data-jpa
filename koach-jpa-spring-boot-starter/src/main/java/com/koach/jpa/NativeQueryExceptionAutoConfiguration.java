package com.koach.jpa;
import com.koach.jpa.exceptions.DefaultNativeQueryException;
import com.koach.jpa.exceptions.MessageResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
@AutoConfiguration
public class NativeQueryExceptionAutoConfiguration {
    @Bean
    public Object nativeQueryExceptionConfigurer(ObjectProvider<MessageResolver> resolverProvider) {
        resolverProvider.ifAvailable(DefaultNativeQueryException::setMessageResolver);
        return new Object();
    }
}
