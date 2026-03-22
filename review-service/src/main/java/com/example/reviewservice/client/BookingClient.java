package com.example.reviewservice.client;

import com.example.common.feign.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "booking-service", configuration = FeignClientConfig.class)
public interface BookingClient {

    @GetMapping(path = "${feign-client.endpoint.was-booked}")
    Boolean wasBooked(@RequestParam("propertyId") Long propertyId, @RequestParam("userId") Long userId);
}
