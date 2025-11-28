package com.example.userservice.controllers;

import com.example.userservice.dto.SaveUserDTO;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private ModelMapper modelMapper;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private Principal principal;

    @InjectMocks
    private UserController userController;

    private UserDTO userDTO;
    private SaveUserDTO saveUserDTO;
    private String authHeader = "Bearer token";

    @BeforeEach
    void setUp() {
        userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@example.com");
        userDTO.setName("Test User");
        userDTO.setPhone("1234567890");

        saveUserDTO = new SaveUserDTO();
        saveUserDTO.setUsername("testuser");
        saveUserDTO.setEmail("test@example.com");
        saveUserDTO.setPassword("password");
        saveUserDTO.setConfirmPassword("password");
        saveUserDTO.setName("Test User");
        saveUserDTO.setPhone("1234567890");
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
        when(userService.updateUserById(anyLong(), any(SaveUserDTO.class), anyString())).thenReturn(userDTO);

        // When
        ResponseEntity<UserDTO> response = userController.updateUser(authHeader, 1L, saveUserDTO, bindingResult);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(userDTO, response.getBody());

        verify(userService, times(1)).updateUserById(1L, saveUserDTO, "token");
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
    void userData_AuthenticatedUser_ReturnsUsername() {
        // Given
        when(principal.getName()).thenReturn("testuser");

        // When
        String response = userController.userData(principal);

        // Then
        assertEquals("testuser", response);
        verify(principal, times(1)).getName();
    }
}