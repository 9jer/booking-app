package com.example.bookingservice.client;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class UserClient {
    private final WebClient webClient;

    public UserClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8084/api/v1/users").build();
    }

    public Mono<Boolean> checkUserExists(Long userId, String jwtToken) {
        return webClient.get()
                .uri("/{id}/exists", userId)
                .headers(headers -> headers.setBearerAuth(jwtToken))
                .retrieve()
                .bodyToMono(Boolean.class);
    }
}
