package com.example.common.feign;

import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@ConditionalOnClass(name = "feign.RequestInterceptor")
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            String authorization = RequestContextHolder.getRequestAttributes() != null ?
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                            .getRequest().getHeader("Authorization") : null;
            if (authorization != null) {
                requestTemplate.header("Authorization", authorization);
            }
        };
    }
}
