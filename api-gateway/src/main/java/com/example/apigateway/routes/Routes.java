package com.example.apigateway.routes;

import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.*;

@Configuration
public class Routes {

    @Bean
    public RouterFunction<ServerResponse> bookingServiceRoute() {
        return GatewayRouterFunctions.route("booking_service")
                .route(RequestPredicates.path("/api/v1/bookings/**"), HandlerFunctions.http("http://localhost:8082"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> propertyServiceRoute() {
        return GatewayRouterFunctions.route("property_service")
                .route(RequestPredicates.path("/api/v1/properties/**"), HandlerFunctions.http("http://localhost:8081"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> reviewServiceRoute() {
        return GatewayRouterFunctions.route("review_service")
                .route(RequestPredicates.path("/api/v1/reviews/**"), HandlerFunctions.http("http://localhost:8083"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> userServiceRoute() {
        return GatewayRouterFunctions.route("user_service")
                .route(RequestPredicates.path("/api/v1/users/**"), HandlerFunctions.http("http://localhost:8084"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> authServiceRoute() {
        return GatewayRouterFunctions.route("auth_service")
                .route(RequestPredicates.path("/api/v1/auth/**"), HandlerFunctions.http("http://localhost:8084"))
                .build();
    }

}
