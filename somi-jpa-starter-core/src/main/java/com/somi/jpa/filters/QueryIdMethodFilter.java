package com.somi.jpa.filters;

import com.somi.jpa.annotations.QueryId;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

/**
 * Matches interfaces declaring at least one {@link QueryId} method. Reads class metadata only,
 * so scanning does not load or initialize any class.
 */
public class QueryIdMethodFilter implements TypeFilter {
    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
        return metadataReader.getClassMetadata().isInterface()
                && metadataReader.getAnnotationMetadata().hasAnnotatedMethods(QueryId.class.getName());
    }
}
