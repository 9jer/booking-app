package com.example.userservice.controllers;

import com.example.userservice.BaseIntegrationTest;
import com.example.userservice.dto.JwtRequest;
import com.example.userservice.dto.SaveUserDTO;
import com.example.userservice.models.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerIT extends BaseIntegrationTest {

    private static final String AUTH_ROOT_ENDPOINT = "/api/v1/auth";
    private static final String SIGN_IN_ENDPOINT = AUTH_ROOT_ENDPOINT + "/sign-in";
    private static final String SIGN_UP_ENDPOINT = AUTH_ROOT_ENDPOINT + "/sign-up";

    @Test
    void signUp_WithValidData_ShouldCreateUserAndReturn200() throws Exception {
        roleRepository.findByName("ROLE_GUEST").orElseGet(() -> {
            com.example.userservice.models.Role guestRole = new com.example.userservice.models.Role();
            guestRole.setName("ROLE_GUEST");
            return roleRepository.save(guestRole);
        });

        SaveUserDTO request = new SaveUserDTO();
        request.setUsername("new_student");
        request.setEmail("student@test.by");
        request.setPassword("strongPassword123");
        request.setConfirmPassword("strongPassword123");
        request.setFirstName("Ivan");
        request.setLastName("Ivanov");
        request.setPhone("+375291112233");

        mockMvc.perform(post(SIGN_UP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new_student"))
                .andExpect(jsonPath("$.email").value("student@test.by"))
                .andExpect(jsonPath("$.id").exists());

        User savedUser = userRepository.findByUsername("new_student").orElseThrow();

        assertThat(savedUser.getEmail()).isEqualTo("student@test.by");
        assertThat(savedUser.getPassword()).isNotEqualTo("strongPassword123");
        assertThat(savedUser.getRoles()).isNotEmpty();
    }

    @Test
    void signUp_WithExistingUsername_ShouldReturnBadRequest() throws Exception {
        createTestUserAndGetToken("existing_user", "password", List.of("ROLE_USER"));

        SaveUserDTO request = new SaveUserDTO();
        request.setUsername("existing_user");
        request.setEmail("another@example.com");
        request.setPassword("pass123");
        request.setConfirmPassword("pass123");

        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPhone("+375299998877");

        mockMvc.perform(post(SIGN_UP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User with username existing_user already exist"));
    }

    @Test
    void signIn_WithValidCredentials_ShouldReturnToken() throws Exception {
        createTestUserAndGetToken("valid_user", "mySecretPass", List.of("ROLE_USER"));

        JwtRequest authRequest = new JwtRequest();
        authRequest.setUsername("valid_user");
        authRequest.setPassword("mySecretPass");

        mockMvc.perform(post(SIGN_IN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void signIn_WithInvalidPassword_ShouldReturnUnauthorized() throws Exception {
        createTestUserAndGetToken("valid_user", "mySecretPass", List.of("ROLE_USER"));

        JwtRequest authRequest = new JwtRequest();
        authRequest.setUsername("valid_user");
        authRequest.setPassword("WRONG_PASSWORD");

        mockMvc.perform(post(SIGN_IN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }
}