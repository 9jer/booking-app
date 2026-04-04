package com.example.userservice.controllers;

import com.example.userservice.BaseIntegrationTest;
import com.example.userservice.dto.UpdateUserDTO;
import com.example.userservice.models.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerIT extends BaseIntegrationTest {

    private static final String USERS_ROOT_ENDPOINT = "/api/v1/users";
    private static final String USERS_ID_ENDPOINT = USERS_ROOT_ENDPOINT + "/{id}";
    private static final String ASSIGN_OWNER_ENDPOINT = USERS_ID_ENDPOINT + "/assign-owner";
    private static final String EXISTS_ENDPOINT = USERS_ID_ENDPOINT + "/exists";

    @Test
    void getAllUsers_ShouldReturnListOfUsers() throws Exception {
        // Given
        String token = createTestUserAndGetToken("list_user", "pass", List.of("ROLE_USER"));

        // When & Then
        mockMvc.perform(get(USERS_ROOT_ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].username").exists());
    }

    @Test
    void getUserById_ShouldReturnUser() throws Exception {
        // Given
        String token = createTestUserAndGetToken("target_user", "pass", List.of("ROLE_USER"));
        User user = userRepository.findByUsername("target_user").orElseThrow();

        // When & Then
        mockMvc.perform(get(USERS_ID_ENDPOINT, user.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.username").value("target_user"));
    }

    @Test
    void updateUserInfo_WithValidData_ShouldReturnUpdatedUser() throws Exception {
        // Given
        String token = createTestUserAndGetToken("update_user", "pass", List.of("ROLE_USER"));

        UpdateUserDTO updateRequest = new UpdateUserDTO();
        updateRequest.setUsername("update_user");
        updateRequest.setEmail("new_email@example.com");
        updateRequest.setFirstName("UpdatedName");
        updateRequest.setLastName("UpdatedLastName");
        updateRequest.setPhone("+375291234567");

        // When
        mockMvc.perform(patch(USERS_ROOT_ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new_email@example.com"))
                .andExpect(jsonPath("$.firstName").value("UpdatedName"));

        // Then
        User updatedUser = userRepository.findByUsername("update_user").orElseThrow();
        assertThat(updatedUser.getEmail()).isEqualTo("new_email@example.com");
        assertThat(updatedUser.getFirstName()).isEqualTo("UpdatedName");
    }

    @Test
    void assignOwnerRole_ShouldReturnUpdatedUser() throws Exception {
        // Given
        String token = createTestUserAndGetToken("future_owner", "pass", List.of("ROLE_ADMIN"));
        User user = userRepository.findByUsername("future_owner").orElseThrow();

        roleRepository.findByName("ROLE_OWNER").orElseGet(() -> {
            com.example.userservice.models.Role ownerRole = new com.example.userservice.models.Role();
            ownerRole.setName("ROLE_OWNER");
            return roleRepository.save(ownerRole);
        });

        // When
        mockMvc.perform(post(ASSIGN_OWNER_ENDPOINT, user.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()));

        // Then
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        boolean hasOwnerRole = updatedUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_OWNER"));
        assertThat(hasOwnerRole).isTrue();
    }

    @Test
    void userExists_ShouldReturnBoolean() throws Exception {
        // Given
        String token = createTestUserAndGetToken("check_user", "pass", List.of("ROLE_USER"));
        User user = userRepository.findByUsername("check_user").orElseThrow();

        // When & Then
        mockMvc.perform(get(EXISTS_ENDPOINT, user.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void getUserById_WhenUserNotFound_ShouldReturnNotFound() throws Exception {
        // Given
        String token = createTestUserAndGetToken("valid_user", "pass", List.of("ROLE_USER"));

        // When & Then
        mockMvc.perform(get(USERS_ID_ENDPOINT, 999999L)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void deleteUser_ByOwner_ShouldDeleteAndReturn200() throws Exception {
        // Given
        String token = createTestUserAndGetToken("user_to_delete", "pass", List.of("ROLE_USER"));
        User user = userRepository.findByUsername("user_to_delete").orElseThrow();

        // When
        mockMvc.perform(delete(USERS_ID_ENDPOINT, user.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Then
        assertThat(userRepository.existsById(user.getId())).isFalse();
    }

    @Test
    void deleteUser_ByAdmin_ShouldDeleteAndReturn200() throws Exception {
        // Given
        createTestUserAndGetToken("target_user", "pass", List.of("ROLE_USER"));
        User targetUser = userRepository.findByUsername("target_user").orElseThrow();

        String adminToken = createTestUserAndGetToken("test_admin", "pass", List.of("ROLE_ADMIN"));

        // When
        mockMvc.perform(delete(USERS_ID_ENDPOINT, targetUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Then
        assertThat(userRepository.existsById(targetUser.getId())).isFalse();
    }

    @Test
    void deleteUser_ByAnotherUser_ShouldReturnBadRequest() throws Exception {
        // Given
        createTestUserAndGetToken("victim_user", "pass", List.of("ROLE_USER"));
        User victimUser = userRepository.findByUsername("victim_user").orElseThrow();

        String hackerToken = createTestUserAndGetToken("hacker_user", "pass", List.of("ROLE_USER"));

        // When & Then
        mockMvc.perform(delete(USERS_ID_ENDPOINT, victimUser.getId())
                        .header("Authorization", "Bearer " + hackerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You can only delete your own account."));

        assertThat(userRepository.existsById(victimUser.getId())).isTrue();
    }
}