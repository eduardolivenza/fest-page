package com.festdescans.api.adapter.in.web;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Configuration;

/** REST adapter: drives the application through its use case interfaces. */
@Configuration(proxyBeanMethods = false)
// Same exclude filter as @SpringBootApplication: test-only configurations are never scanned.
@ComponentScan(basePackageClasses = WebAdapterConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class))
@EnableConfigurationProperties(WebProperties.class)
public class WebAdapterConfiguration {
}
