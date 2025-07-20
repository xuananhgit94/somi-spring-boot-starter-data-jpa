package com.koach.jpa.filters;

import com.koach.jpa.anotations.QueryId;
import jakarta.annotation.Nullable;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import java.lang.reflect.Method;

public class QueryIdMethodFilter implements TypeFilter {
    @Override
    public boolean match(MetadataReader metadataReader,@Nullable MetadataReaderFactory metadataReaderFactory) {
        String className = metadataReader.getClassMetadata().getClassName();
        try {
            Class<?> clazz = Class.forName(className);
            if (!clazz.isInterface()) return false;

            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(QueryId.class)) {
                    return true;
                }
            }
        } catch (ClassNotFoundException ignored) {}
        return false;
    }
}
