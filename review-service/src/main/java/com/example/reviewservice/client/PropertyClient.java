package com.example.reviewservice.client;

import com.example.common.feign.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "property-service", configuration = FeignClientConfig.class)
public interface PropertyClient {

    @GetMapping(path = "${feign-client.endpoint.property-exists}")
    Boolean propertyExists(@PathVariable("id") Long id);
}
