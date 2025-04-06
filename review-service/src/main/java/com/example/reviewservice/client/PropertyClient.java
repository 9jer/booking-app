package com.example.reviewservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("property-service")
public interface PropertyClient {

    @GetMapping(path = "${feign-client.endpoint.property-exists}")
    Boolean propertyExists(@PathVariable("id") Long id);
}
