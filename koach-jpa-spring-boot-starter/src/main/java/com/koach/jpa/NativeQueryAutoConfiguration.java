package com.koach.jpa;
import com.koach.jpa.configs.QueryMapperProperties;
import com.koach.jpa.executors.QueryByIdExecutor;
import com.koach.jpa.providers.QueryByIdProvider;
import com.koach.jpa.providers.SafeEntityManagerProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
@AutoConfigureAfter(HibernateJpaAutoConfiguration.class)
@EnableConfigurationProperties(QueryMapperProperties.class)
public class NativeQueryAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public QueryByIdProvider queryByIdProvider(QueryMapperProperties properties) {
        return new QueryByIdProvider(properties.getPathQueries());
    }
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(QueryByIdProvider.class)
    public QueryByIdExecutor queryByIdExecutor(QueryByIdProvider provider) {
        return new QueryByIdExecutor(provider);
    }
    @Bean
    public SafeEntityManagerProvider safeEntityManagerProvider() {
        return new SafeEntityManagerProvider();
    }
}
