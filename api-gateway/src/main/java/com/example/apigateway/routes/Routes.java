package com.example.apigateway.routes;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Routes {

    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("booking_service", r -> r.path("/api/v1/bookings/**")
                        .uri("http://booking-service:8080"))

                .route("booking_service_swagger", r -> r.path("/aggregate/booking-service/v3/api-docs")
                        .filters(f -> f.rewritePath("/aggregate/booking-service/v3/api-docs", "/v3/api-docs"))
                        .uri("http://booking-service:8080"))

                .route("property_service", r -> r.path("/api/v1/properties/**")
                        .uri("http://property-service:8080"))

                .route("image_service", r -> r.path("/api/v1/images/**")
                        .uri("http://property-service:8080"))

                .route("features_service", r -> r.path("/api/v1/features/**")
                        .uri("http://property-service:8080"))

                .route("property_service_swagger", r -> r.path("/aggregate/property-service/v3/api-docs")
                        .filters(f -> f.rewritePath("/aggregate/property-service/v3/api-docs", "/v3/api-docs"))
                        .uri("http://property-service:8080"))

                .route("review_service", r -> r.path("/api/v1/reviews/**")
                        .uri("http://review-service:8080"))

                .route("review_service_swagger", r -> r.path("/aggregate/review-service/v3/api-docs")
                        .filters(f -> f.rewritePath("/aggregate/review-service/v3/api-docs", "/v3/api-docs"))
                        .uri("http://review-service:8080"))

                .route("user_service", r -> r.path("/api/v1/users/**")
                        .uri("http://user-service:8080"))

                .route("auth_service", r -> r.path("/api/v1/auth/**")
                        .uri("http://user-service:8080"))

                .route("user_service_swagger", r -> r.path("/aggregate/user-service/v3/api-docs")
                        .filters(f -> f.rewritePath("/aggregate/user-service/v3/api-docs", "/v3/api-docs"))
                        .uri("http://user-service:8080"))

                .build();
    }
}
