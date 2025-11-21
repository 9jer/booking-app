package com.example.userservice.controllers;

import com.example.userservice.dto.SaveUserDTO;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.models.User;
import com.example.userservice.services.UserService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerIT {

    private static final String USERS_ROOT_ENDPOINT = "/api/v1/users";
    private static final String USERS_ID_ENDPOINT = USERS_ROOT_ENDPOINT + "/{id}";
    private static final String ASSIGN_OWNER_ENDPOINT = USERS_ID_ENDPOINT + "/assign-owner";
    private static final String EXISTS_ENDPOINT = USERS_ID_ENDPOINT + "/exists";
    private static final String INFO_ENDPOINT = USERS_ROOT_ENDPOINT + "/info";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenUtils jwtTokenUtils;

    @MockBean
    private ModelMapper modelMapper;

    private User testUser;
    private UserDTO testUserDTO;
    private SaveUserDTO testSaveUserDTO;
    private String validToken = "valid.token.here";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setPhone("1234567890");
        testUser.setCreatedAt(LocalDateTime.now());

        testUserDTO = new UserDTO();
        testUserDTO.setId(1L);
        testUserDTO.setUsername("testuser");
        testUserDTO.setEmail("test@example.com");
        testUserDTO.setName("Test User");
        testUserDTO.setPhone("1234567890");

        testSaveUserDTO = new SaveUserDTO();
        testSaveUserDTO.setUsername("testuser");
        testSaveUserDTO.setEmail("test@example.com");
        testSaveUserDTO.setPassword("password");
        testSaveUserDTO.setConfirmPassword("password");
        testSaveUserDTO.setName("Test User");
        testSaveUserDTO.setPhone("1234567890");

        Mockito.when(jwtTokenUtils.getUsername(anyString())).thenReturn("testuser");
        Mockito.when(jwtTokenUtils.getUserId(anyString())).thenReturn(1L);
        Mockito.when(jwtTokenUtils.getRoles(anyString())).thenReturn(List.of("ROLE_USER"));
    }

    @Test
    void getAllUsers_ShouldReturnListOfUsers() throws Exception {
        Mockito.when(userService.findAll()).thenReturn(List.of(testUser));
        Mockito.when(modelMapper.map(testUser, UserDTO.class)).thenReturn(testUserDTO);

        mockMvc.perform(get(USERS_ROOT_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].id").value(testUserDTO.getId()))
                .andExpect(jsonPath("$.users[0].username").value(testUserDTO.getUsername()));
    }

    @Test
    void getUserById_ShouldReturnUser() throws Exception {
        Mockito.when(userService.getUserById(anyLong())).thenReturn(testUser);
        Mockito.when(modelMapper.map(testUser, UserDTO.class)).thenReturn(testUserDTO);

        mockMvc.perform(get(USERS_ID_ENDPOINT, 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserDTO.getId()))
                .andExpect(jsonPath("$.username").value(testUserDTO.getUsername()));
    }

    @Test
    void getUserById_WhenNotFound_ShouldReturnNotFound() throws Exception {
        Mockito.when(userService.getUserById(anyLong()))
                .thenThrow(new UserException("User not found"));

        mockMvc.perform(get(USERS_ID_ENDPOINT, 999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void updateUser_WithValidData_ShouldReturnUpdatedUser() throws Exception {
        Mockito.when(userService.updateUserById(anyLong(), any(SaveUserDTO.class)))
                .thenReturn(testUser);
        Mockito.when(modelMapper.map(testUser, UserDTO.class)).thenReturn(testUserDTO);

        mockMvc.perform(patch(USERS_ID_ENDPOINT, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testSaveUserDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserDTO.getId()));
    }

    @Test
    void updateUser_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        SaveUserDTO invalidUserDTO = new SaveUserDTO();
        invalidUserDTO.setUsername("");

        mockMvc.perform(patch(USERS_ID_ENDPOINT, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUserDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUser_ShouldReturnOkStatus() throws Exception {
        mockMvc.perform(delete(USERS_ID_ENDPOINT, 1L))
                .andExpect(status().isOk());
    }

    @Test
    void assignOwnerRole_ShouldReturnUpdatedUser() throws Exception {
        Mockito.when(userService.assignOwnerRole(anyLong())).thenReturn(testUser);
        Mockito.when(modelMapper.map(testUser, UserDTO.class)).thenReturn(testUserDTO);

        mockMvc.perform(post(ASSIGN_OWNER_ENDPOINT, 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserDTO.getId()));
    }

    @Test
    void userExists_ShouldReturnBoolean() throws Exception {
        Mockito.when(userService.existsById(anyLong())).thenReturn(true);

        mockMvc.perform(get(EXISTS_ENDPOINT, 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void userData_ShouldReturnUsername() throws Exception {
        mockMvc.perform(get(INFO_ENDPOINT)
                        .principal(() -> "testuser"))
                .andExpect(status().isOk())
                .andExpect(content().string("testuser"));
    }

    @Test
    void updateUser_WhenUserNotFound_ShouldReturnNotFound() throws Exception {
        Mockito.when(userService.updateUserById(anyLong(), any(SaveUserDTO.class)))
                .thenThrow(new UserException("User not found"));

        mockMvc.perform(patch(USERS_ID_ENDPOINT, 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testSaveUserDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void assignOwnerRole_WhenUserNotFound_ShouldReturnNotFound() throws Exception {
        Mockito.when(userService.assignOwnerRole(anyLong()))
                .thenThrow(new UserException("User not found"));

        mockMvc.perform(post(ASSIGN_OWNER_ENDPOINT, 999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User not found"));
    }
}