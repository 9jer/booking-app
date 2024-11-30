package com.example.propertyservice.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;

@Service
public class BookingClient {

    private final WebClient webClient;

    public BookingClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8082/api/v1/bookings").build();
    }

    public Boolean isPropertyAvailable(Long propertyId, LocalDate checkIn, LocalDate checkOut, String jwtToken) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/availability")
                        .queryParam("propertyId", propertyId)
                        .queryParam("checkIn", checkIn)
                        .queryParam("checkOut", checkOut)
                        .build())
                .headers(headers -> headers.setBearerAuth(jwtToken))
                .retrieve()
                .bodyToMono(Boolean.class)
                .block();
    }

    public List<LocalDate> getAvailableDates(Long propertyId, String jwtToken) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/available-dates")
                        .queryParam("propertyId", propertyId)
                        .build())
                .headers(headers -> headers.setBearerAuth(jwtToken))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<LocalDate>>() {})
                .block();
    }
}
