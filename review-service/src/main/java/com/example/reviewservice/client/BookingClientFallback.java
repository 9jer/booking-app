package com.example.reviewservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BookingClientFallback implements BookingClient {

    @Override
    public Boolean wasBooked(Long propertyId, Long userId) {
        log.error("Circuit Breaker OPEN: booking-service is unavailable. Fallback for wasBooked(propertyId={}, userId={})", propertyId, userId);
        return false;
    }
}