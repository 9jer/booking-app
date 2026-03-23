package com.example.reviewservice.client;

import com.example.reviewservice.dto.UserResponseDTO;
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

    @Override
    public UserResponseDTO getUserById(Long id) {
        log.error("Circuit Breaker OPEN: user-service is unavailable. Fallback for getUserById({})", id);
        return new UserResponseDTO();
    }
}