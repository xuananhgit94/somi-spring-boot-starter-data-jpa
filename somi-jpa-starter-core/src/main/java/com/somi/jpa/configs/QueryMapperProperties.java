package com.somi.jpa.configs;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix = "queries")
@Data
public class QueryMapperProperties {
    private String[] pathQueries = {"classpath:/queries/**/*.xml"};
}
