package com.festdescans.api.adapter.in.web;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/** REST adapter: drives the application through its use case interfaces. */
@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackageClasses = WebAdapterConfiguration.class)
@EnableConfigurationProperties(WebProperties.class)
// Module defaults (OpenAPI paths); application.yml and env vars override them.
@PropertySource("classpath:web-adapter.properties")
public class WebAdapterConfiguration {
}
