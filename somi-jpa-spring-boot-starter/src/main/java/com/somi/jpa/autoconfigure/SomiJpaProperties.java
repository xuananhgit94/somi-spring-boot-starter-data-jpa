package com.somi.jpa.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Configuration of the Somi native query starter.
 *
 * @param queryLocations resource patterns of the XML query files
 */
@ConfigurationProperties("somi.jpa")
public record SomiJpaProperties(@DefaultValue("classpath*:/queries/**/*.xml") List<String> queryLocations) {
}
