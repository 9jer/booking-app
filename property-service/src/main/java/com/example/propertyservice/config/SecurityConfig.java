package com.example.propertyservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtRequestFilter jwtRequestFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/images/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/features/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/properties/my").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/properties/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/properties").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/properties").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/properties/{id}").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/properties/{id}").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler(customAccessDeniedHandler)
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
