package com.example.reviewservice.client;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class PropertyClient {
    private final WebClient webClient;

    public PropertyClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8081/api/v1/properties").build();;
    }

    public Mono<Boolean> checkPropertyExists(Long propertyId, String jwtToken) {
        return webClient.get()
                .uri("/{id}/exists", propertyId)
                .headers(headers -> headers.setBearerAuth(jwtToken))
                .retrieve()
                .bodyToMono(Boolean.class);
    }
}
