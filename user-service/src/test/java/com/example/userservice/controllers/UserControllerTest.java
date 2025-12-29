package com.example.userservice.controllers;

import com.example.userservice.dto.UpdateUserDTO;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.security.CustomUserDetailsService;
import com.example.userservice.services.UserService;
import com.example.userservice.util.JwtTokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;

import java.security.Principal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private Principal principal;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private JwtTokenUtils jwtTokenUtils;

    @InjectMocks
    private UserController userController;

    private UserDTO userDTO;
    private UpdateUserDTO updateUserDTO;
    private String authHeader = "Bearer token";

    @BeforeEach
    void setUp() {
        userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@example.com");
        userDTO.setFirstName("Test");
        userDTO.setLastName("User");
        userDTO.setPhone("1234567890");

        updateUserDTO = new UpdateUserDTO();
        updateUserDTO.setUsername("testuser");
        updateUserDTO.setEmail("test@example.com");
        updateUserDTO.setFirstName("Test");
        updateUserDTO.setLastName("User");
        updateUserDTO.setPhone("1234567890");
    }

    @Test
    void getAllUsers_ReturnsPageOfUsers() {
        // Given
        when(userService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(userDTO)));

        // When
        ResponseEntity<Page<UserDTO>> response = userController.getAllUsers(Pageable.unpaged());

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());

        assertEquals(1, response.getBody().getTotalElements());
        assertEquals(userDTO, response.getBody().getContent().get(0));

        verify(userService, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void getUserById_UserExists_ReturnsUserDTO() {
        // Given
        when(userService.getUserById(1L)).thenReturn(userDTO);

        // When
        ResponseEntity<UserDTO> response = userController.getUserById(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(userDTO, response.getBody());

        verify(userService, times(1)).getUserById(1L);
    }

    @Test
    void updateUser_ValidRequest_ReturnsUpdatedUser() {
        // Given
        when(bindingResult.hasErrors()).thenReturn(false);

        UserDetails mockUserDetails = org.springframework.security.core.userdetails.User
                .withUsername("testuser")
                .password("password")
                .authorities("ROLE_USER")
                .build();

        lenient().when(userDetailsService.loadUserByUsername(any())).thenReturn(mockUserDetails);
        lenient().when(jwtTokenUtils.generateToken(any(UserDetails.class))).thenReturn("mock-generated-token");
        when(userService.updateUser(any(UpdateUserDTO.class), anyString())).thenReturn(userDTO);

        // When
        ResponseEntity<UserDTO> response = userController.updateUserInfo(authHeader, updateUserDTO, bindingResult);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(userDTO, response.getBody());

        verify(userService, times(1)).updateUser(updateUserDTO, "token");
    }

    @Test
    void assignOwnerRole_ValidId_ReturnsUserDTO() {
        // Given
        when(userService.assignOwnerRole(1L)).thenReturn(userDTO);

        // When
        ResponseEntity<UserDTO> response = userController.assignOwnerRole(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(userDTO, response.getBody());

        verify(userService, times(1)).assignOwnerRole(1L);
    }

    @Test
    void getCurrentUser_Authenticated_ReturnsUserDTO() {
        // Given
        String username = "testuser";
        when(principal.getName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(userDTO);

        // When
        ResponseEntity<UserDTO> response = userController.getUserProfile(principal);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userDTO, response.getBody());
    }
}