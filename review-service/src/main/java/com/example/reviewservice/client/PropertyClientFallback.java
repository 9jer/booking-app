package com.example.reviewservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PropertyClientFallback implements PropertyClient {

    @Override
    public Boolean propertyExists(Long id) {
        log.error("Circuit Breaker OPEN: property-service is unavailable. Fallback for propertyExists({})", id);
        return false;
    }
}