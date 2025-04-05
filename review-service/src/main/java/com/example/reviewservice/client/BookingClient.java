package com.example.reviewservice.client;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class BookingClient {

    private final WebClient webClient;

    public BookingClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("http://localhost:8082/api/v1/bookings").build();
    }

    public Mono<Boolean> checkIfItWasBooked(Long propertyId, Long userId, String jwtToken) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/was-booked")
                        .queryParam("propertyId", propertyId)
                        .queryParam("userId", userId)
                        .build())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(jwtToken))
                .retrieve()
                .bodyToMono(Boolean.class);
    }
}
