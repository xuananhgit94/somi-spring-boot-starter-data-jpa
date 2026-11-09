package com.somi.jpa.registers;
import com.somi.jpa.annotations.EnableNativeQueries;
import com.somi.jpa.constants.AppConstants;
import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.exceptions.DefaultNativeQueryException;
import com.somi.jpa.factories.NativeQueryRepositoryFactoryBean;
import com.somi.jpa.filters.QueryIdMethodFilter;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.beans.Introspector;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class NativeQueryRepositoryRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {
    private ResourceLoader resourceLoader;
    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        Set<String> basePackages = resolveBasePackages(metadata);
        ClassPathScanningCandidateComponentProvider scanner = createScanner();
        scanner.setResourceLoader(resourceLoader);
        for (String basePackage : basePackages) {
            for (BeanDefinition beanDefinition : scanner.findCandidateComponents(basePackage)) {
                try {
                    registerRepository(beanDefinition, registry);
                } catch (Exception e) {
                    DefaultNativeQueryException.fail(e, ErrorMessage.FAILED_TO_REGISTER, beanDefinition.getBeanClassName());
                }
            }
        }
    }

    private void registerRepository(BeanDefinition bd, BeanDefinitionRegistry registry) throws ClassNotFoundException {
        String className = bd.getBeanClassName();
        if (!StringUtils.hasText(className)) {
            return;
        }
        Class<?> candidate = Class.forName(className);
        if (!candidate.isInterface()) {
            DefaultNativeQueryException.fail(ErrorMessage.MUST_BE_INTERFACE, className);
        }
        String beanName = Introspector.decapitalize(candidate.getSimpleName());
        if (registry.containsBeanDefinition(beanName)) {
            return;
        }
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(NativeQueryRepositoryFactoryBean.class).addConstructorArgValue(candidate);
        registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
    }
    private ClassPathScanningCandidateComponentProvider createScanner() {
        return new ClassPathScanningCandidateComponentProvider(false) {
            {
                addIncludeFilter(new QueryIdMethodFilter());
            }
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isInterface();
            }
        };
    }

    private Set<String> resolveBasePackages(AnnotationMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(EnableNativeQueries.class.getName());
        if (!CollectionUtils.isEmpty(attributes)) {
            String[] basePackages = (String[]) attributes.get(AppConstants.BASE_PACKAGE_SCAN);
            if (basePackages != null && basePackages.length > 0) {
                return Arrays.stream(basePackages).filter(StringUtils::hasText).collect(Collectors.toSet());
            }
        }
        return Set.of(metadata.getClassName().substring(0, metadata.getClassName().lastIndexOf('.')));
    }
}
