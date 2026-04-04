package com.example.propertyservice.client;

import com.example.propertyservice.dto.AvailableDatesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;

@Component
@Slf4j
public class BookingClientFallback implements BookingClient {

    @Override
    public Boolean isAvailable(Long propertyId, LocalDate checkIn, LocalDate checkOut) {
        log.error("Circuit Breaker OPEN: booking-service is unavailable. Fallback for isAvailable propertyId: {}", propertyId);
        return false;
    }

    @Override
    public AvailableDatesResponse getAvailableDates(Long propertyId) {
        log.error("Circuit Breaker OPEN: booking-service is unavailable. Fallback for getAvailableDates propertyId: {}", propertyId);
        AvailableDatesResponse fallbackResponse = new AvailableDatesResponse();
        fallbackResponse.setAvailableDates(Collections.emptyList());
        return fallbackResponse;
    }
}