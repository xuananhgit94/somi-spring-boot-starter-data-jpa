package com.somi.jpa.autoconfigure;

import com.somi.jpa.executors.QueryByIdExecutor;
import com.somi.jpa.mappers.converters.ValueConversionService;
import com.somi.jpa.providers.QueryByIdProvider;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.query.NativeQuery;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration(after = HibernateJpaAutoConfiguration.class)
@ConditionalOnClass({EntityManagerFactory.class, NativeQuery.class})
@ConditionalOnSingleCandidate(EntityManagerFactory.class)
@EnableConfigurationProperties(SomiJpaProperties.class)
public class SomiJpaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public QueryByIdProvider queryByIdProvider(SomiJpaProperties properties, ResourceLoader resourceLoader) {
        return new QueryByIdProvider(properties.queryLocations(), ResourcePatternUtils.getResourcePatternResolver(resourceLoader));
    }

    /**
     * Uses the application's {@link JsonMapper} when Spring Boot's Jackson support is present, so
     * {@code spring.jackson.*} settings also apply to JSON columns.
     */
    @Bean
    @ConditionalOnMissingBean
    public ValueConversionService somiValueConversionService(ObjectProvider<JsonMapper> jsonMapper) {
        return new ValueConversionService(jsonMapper.getIfAvailable(() -> JsonMapper.builder().build()));
    }

    @Bean
    @ConditionalOnMissingBean
    public QueryByIdExecutor queryByIdExecutor(QueryByIdProvider queryByIdProvider,
                                               EntityManagerFactory entityManagerFactory,
                                               ValueConversionService somiValueConversionService) {
        return new QueryByIdExecutor(queryByIdProvider,
                SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory),
                somiValueConversionService);
    }
}
