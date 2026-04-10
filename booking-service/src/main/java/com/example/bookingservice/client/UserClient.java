package com.example.bookingservice.client;

import com.example.bookingservice.dto.UserDTO;
import com.example.common.feign.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "user-service",
        url = "${feign-client.url.user-service:http://user-service:8080}",
        configuration = FeignClientConfig.class,
        fallback = UserClientFallback.class
)
public interface UserClient {

    @GetMapping(path = "${feign-client.endpoint.user-exists}")
    Boolean userExists(@PathVariable("id") Long id);

    @GetMapping(path = "${feign-client.endpoint.get-user-by-id}")
    UserDTO getUserById(@PathVariable("id") Long id);
}
