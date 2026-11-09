package com.somi.jpa.configs;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix = "queries")
public class QueryMapperProperties {
    private String[] pathQueries = {"classpath:/queries/**/*.xml"};

    public String[] getPathQueries() {
        return pathQueries;
    }

    public void setPathQueries(String[] pathQueries) {
        this.pathQueries = pathQueries;
    }
}
