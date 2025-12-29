package com.example.bookingservice.client;

import com.example.bookingservice.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("user-service")
public interface UserClient {

    @GetMapping(path = "${feign-client.endpoint.user-exists}")
    Boolean userExists(@PathVariable("id") Long id);

    @GetMapping(path = "/api/v1/users/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);
}
