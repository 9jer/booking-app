package com.example.propertyservice.client;

import com.example.common.feign.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", configuration = FeignClientConfig.class)
public interface UserClient {

    @GetMapping(path = "${feign-client.endpoint.user-exists}")
    Boolean userExists(@PathVariable("id") Long id);
}
