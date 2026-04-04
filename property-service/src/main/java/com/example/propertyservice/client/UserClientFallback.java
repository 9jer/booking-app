package com.example.propertyservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserClientFallback implements UserClient {

    @Override
    public Boolean userExists(Long id) {
        log.error("Circuit Breaker OPEN: user-service is unavailable. Fallback for userExists({})", id);
        return false;
    }
}