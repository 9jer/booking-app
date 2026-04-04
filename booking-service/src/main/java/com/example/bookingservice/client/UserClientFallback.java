package com.example.bookingservice.client;

import com.example.bookingservice.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserClientFallback implements UserClient{

    @Override
    public Boolean userExists(Long id) {
        log.error("Circuit Breaker OPEN: user-service is unavailable. Fallback for userExists({})", id);
        return false;
    }

    @Override
    public UserDTO getUserById(Long id) {
        log.error("Circuit Breaker OPEN: user-service is unavailable. Fallback for getUserById({})", id);
        return new UserDTO();
    }
}