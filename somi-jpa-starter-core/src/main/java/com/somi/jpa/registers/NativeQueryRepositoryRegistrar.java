package com.somi.jpa.registers;

import com.somi.jpa.annotations.EnableNativeQueries;
import com.somi.jpa.constants.AppConstants;
import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.exceptions.NativeQueryException;
import com.somi.jpa.factories.NativeQueryRepositoryFactoryBean;
import com.somi.jpa.filters.QueryIdMethodFilter;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registers a {@link NativeQueryRepositoryFactoryBean} for every interface with {@code @QueryId} methods
 * found under the packages of {@link EnableNativeQueries}.
 * <p>
 * Kept on {@link ImportBeanDefinitionRegistrar} rather than Spring 7's {@code BeanRegistrar} because it
 * needs the metadata of the importing {@code @EnableNativeQueries} class.
 */
@SuppressWarnings("NullAway.Init") // fields are set through the Aware callbacks before registration
public class NativeQueryRepositoryRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {
    private ResourceLoader resourceLoader;
    private Environment environment;

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        ClassPathScanningCandidateComponentProvider scanner = createScanner();
        for (String basePackage : resolveBasePackages(metadata)) {
            for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
                String className = candidate.getBeanClassName();
                if (className == null) continue;
                try {
                    registerRepository(ClassUtils.forName(className, resourceLoader.getClassLoader()), registry);
                } catch (ClassNotFoundException | LinkageError e) {
                    throw NativeQueryException.wrap(e, ErrorMessage.FAILED_TO_REGISTER, className);
                }
            }
        }
    }

    private static void registerRepository(Class<?> repositoryInterface, BeanDefinitionRegistry registry) {
        String beanName = StringUtils.uncapitalizeAsProperty(repositoryInterface.getSimpleName());
        if (registry.containsBeanDefinition(beanName)) {
            return;
        }
        AbstractBeanDefinition definition = BeanDefinitionBuilder
                .genericBeanDefinition(NativeQueryRepositoryFactoryBean.class)
                .addConstructorArgValue(repositoryInterface)
                // Remaining constructor argument (QueryByIdExecutor) is autowired by type
                .setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR)
                .getBeanDefinition();
        // Lets the container match the repository type without instantiating the factory early
        definition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, repositoryInterface);
        registry.registerBeanDefinition(beanName, definition);
    }

    private ClassPathScanningCandidateComponentProvider createScanner() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false, environment) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
            }
        };
        scanner.setResourceLoader(resourceLoader);
        scanner.addIncludeFilter(new QueryIdMethodFilter());
        return scanner;
    }

    private static Set<String> resolveBasePackages(AnnotationMetadata metadata) {
        @Nullable Map<String, @Nullable Object> attributes = metadata.getAnnotationAttributes(EnableNativeQueries.class.getName());
        if (attributes != null && attributes.get(AppConstants.BASE_PACKAGE_SCAN) instanceof String[] basePackages) {
            Set<String> packages = Arrays.stream(basePackages).filter(StringUtils::hasText).collect(Collectors.toSet());
            if (!packages.isEmpty()) {
                return packages;
            }
        }
        return Set.of(ClassUtils.getPackageName(metadata.getClassName()));
    }
}
