package com.example.common.config;

import com.example.common.security.ReactiveAccessDeniedHandler;
import com.example.common.security.ReactiveAuthenticationEntryPoint;
import com.example.common.security.ServletAccessDeniedHandler;
import com.example.common.security.ServletAuthenticationEntryPoint;
import com.example.common.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class CommonSecurityAutoConfiguration {

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public ServletAccessDeniedHandler servletAccessDeniedHandler() {
        return new ServletAccessDeniedHandler();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public ServletAuthenticationEntryPoint servletAuthenticationEntryPoint() {
        return new ServletAuthenticationEntryPoint();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public ReactiveAccessDeniedHandler reactiveAccessDeniedHandler() {
        return new ReactiveAccessDeniedHandler();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public ReactiveAuthenticationEntryPoint reactiveAuthenticationEntryPoint() {
        return new ReactiveAuthenticationEntryPoint();
    }
}