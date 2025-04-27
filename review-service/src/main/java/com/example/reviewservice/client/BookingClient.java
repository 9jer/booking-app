package com.example.reviewservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("booking-service")
public interface BookingClient {

    @GetMapping(path = "${feign-client.endpoint.was-booked}")
    Boolean wasBooked(@RequestParam("propertyId") Long propertyId, @RequestParam("userId") Long userId);
}
