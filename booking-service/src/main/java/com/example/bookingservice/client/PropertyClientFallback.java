package com.example.bookingservice.client;

import com.example.bookingservice.dto.GetPropertyDTO;
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

    @Override
    public GetPropertyDTO getPropertyById(Long id) {
        log.error("Circuit Breaker OPEN: property-service is unavailable. Fallback for getPropertyById({})", id);
        GetPropertyDTO fallbackProperty = new GetPropertyDTO();
        fallbackProperty.setId(id);
        fallbackProperty.setTitle("Property details currently unavailable");
        return fallbackProperty;
    }
}