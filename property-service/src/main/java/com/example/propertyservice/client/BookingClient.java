package com.example.propertyservice.client;

import com.example.propertyservice.dto.AvailableDatesResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@FeignClient("booking-service")
public interface BookingClient {

    @GetMapping(path = "${feign-client.endpoint.property-availability}")
    Boolean isAvailable(@RequestParam("propertyId") Long propertyId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut);

    @GetMapping(path = "${feign-client.endpoint.available-dates}")
    AvailableDatesResponse getAvailableDates(@RequestParam Long propertyId);
}
