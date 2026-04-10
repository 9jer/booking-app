package com.example.bookingservice.client;

import com.example.bookingservice.dto.GetPropertyDTO;
import com.example.common.feign.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "property-service",
        url = "${feign-client.url.property-service:http://property-service:8080}",
        configuration = FeignClientConfig.class,
        fallback = PropertyClientFallback.class
)
public interface PropertyClient {

    @GetMapping(path = "${feign-client.endpoint.property-exists}")
    Boolean propertyExists(@PathVariable("id") Long id);

    @GetMapping(path = "${feign-client.endpoint.get-property-by-id}")
    GetPropertyDTO getPropertyById(@PathVariable("id") Long id);
}
