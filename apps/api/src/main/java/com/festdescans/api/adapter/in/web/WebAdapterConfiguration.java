package com.festdescans.api.adapter.in.web;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/** REST adapter: drives the application through its use case interfaces. */
@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackageClasses = WebAdapterConfiguration.class)
@EnableConfigurationProperties(WebProperties.class)
public class WebAdapterConfiguration {
}
