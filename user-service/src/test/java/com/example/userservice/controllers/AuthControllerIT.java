package com.example.userservice.controllers;

import com.example.userservice.dto.JwtRequest;
import com.example.userservice.dto.JwtResponse;
import com.example.userservice.dto.SaveUserDTO;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.models.User;
import com.example.userservice.services.AuthService;
import com.example.userservice.util.AuthException;
import com.example.userservice.util.JwtTokenUtils;
import com.example.userservice.util.UserException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerIT {

    private static final String AUTH_ROOT_ENDPOINT = "/api/v1/auth";
    private static final String SIGN_IN_ENDPOINT = AUTH_ROOT_ENDPOINT + "/sign-in";
    private static final String SIGN_UP_ENDPOINT = AUTH_ROOT_ENDPOINT + "/sign-up";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenUtils jwtTokenUtils;

    @MockBean
    private ModelMapper modelMapper;

    private JwtRequest testJwtRequest;
    private SaveUserDTO testSaveUserDTO;
    private JwtResponse testJwtResponse;
    private User testUser;
    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        testJwtRequest = new JwtRequest();
        testJwtRequest.setUsername("testuser");
        testJwtRequest.setPassword("password");

        testSaveUserDTO = new SaveUserDTO();
        testSaveUserDTO.setUsername("testuser");
        testSaveUserDTO.setEmail("test@example.com");
        testSaveUserDTO.setPassword("password");
        testSaveUserDTO.setConfirmPassword("password");
        testSaveUserDTO.setName("Test User");
        testSaveUserDTO.setPhone("1234567890");

        testJwtResponse = new JwtResponse("test.token.here");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testUserDTO = new UserDTO();
        testUserDTO.setId(1L);
        testUserDTO.setUsername("testuser");
        testUserDTO.setEmail("test@example.com");

        Mockito.when(jwtTokenUtils.getUsername(anyString())).thenReturn("testuser");
        Mockito.when(jwtTokenUtils.getUserId(anyString())).thenReturn(1L);
        Mockito.when(jwtTokenUtils.getRoles(anyString())).thenReturn(List.of("ROLE_USER"));
    }

    @Test
    void createAuthToken_WithValidCredentials_ShouldReturnToken() throws Exception {
        Mockito.when(authService.createAuthToken(any(JwtRequest.class)))
                .thenReturn(testJwtResponse);

        mockMvc.perform(post(SIGN_IN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testJwtRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(testJwtResponse.getToken()));
    }

    @Test
    void createAuthToken_WithInvalidCredentials_ShouldReturnUnauthorized() throws Exception {
        Mockito.when(authService.createAuthToken(any(JwtRequest.class)))
                .thenThrow(new AuthException("Incorrect login or password"));

        mockMvc.perform(post(SIGN_IN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testJwtRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Incorrect login or password"));
    }

    @Test
    void createAuthToken_WithInvalidInput_ShouldReturnBadRequest() throws Exception {
        JwtRequest invalidRequest = new JwtRequest();
        invalidRequest.setUsername("");
        invalidRequest.setPassword("");

        mockMvc.perform(post(SIGN_IN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createNewUser_WithValidData_ShouldReturnCreatedUser() throws Exception {
        Mockito.when(authService.createNewUser(any(SaveUserDTO.class)))
                .thenReturn(testUser);
        Mockito.when(modelMapper.map(testUser, UserDTO.class)).thenReturn(testUserDTO);

        mockMvc.perform(post(SIGN_UP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testSaveUserDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserDTO.getId()))
                .andExpect(jsonPath("$.username").value(testUserDTO.getUsername()))
                .andExpect(jsonPath("$.email").value(testUserDTO.getEmail()));
    }

    @Test
    void createNewUser_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        SaveUserDTO invalidUserDTO = new SaveUserDTO();
        invalidUserDTO.setUsername("");
        invalidUserDTO.setEmail("invalid-email");

        mockMvc.perform(post(SIGN_UP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUserDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createNewUser_WithExistingUsername_ShouldReturnBadRequest() throws Exception {
        Mockito.when(authService.createNewUser(any(SaveUserDTO.class)))
                .thenThrow(new UserException("Username already exists"));

        mockMvc.perform(post(SIGN_UP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testSaveUserDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    void createNewUser_WithPasswordMismatch_ShouldReturnBadRequest() throws Exception {
        testSaveUserDTO.setConfirmPassword("differentpassword");

        Mockito.when(authService.createNewUser(any(SaveUserDTO.class)))
                .thenThrow(new UserException("Incorrect password!"));

        mockMvc.perform(post(SIGN_UP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testSaveUserDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Incorrect password!"));
    }

    @Test
    void createNewUser_WithInvalidEmail_ShouldReturnBadRequest() throws Exception {
        testSaveUserDTO.setEmail("invalid-email");

        mockMvc.perform(post(SIGN_UP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testSaveUserDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}