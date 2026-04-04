package com.example.common.logging;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
public class LoggingAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = "jakarta.servlet.Filter")
    public static class ServletLoggingConfig {

        @Bean
        public ServletLoggingFilter servletLoggingFilter() {
            return new ServletLoggingFilter();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnClass(name = "org.springframework.web.server.WebFilter")
    public static class ReactiveLoggingConfig {

        @Bean
        public ReactiveLoggingFilter reactiveLoggingFilter() {
            return new ReactiveLoggingFilter();
        }
    }
}