package com.example.userservice.controllers;

import com.example.userservice.dto.JwtRequest;
import com.example.userservice.dto.JwtResponse;
import com.example.userservice.dto.SaveUserDTO;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.models.User;
import com.example.userservice.services.AuthService;
import com.example.userservice.util.UserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private Logger logger;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private AuthController authController;

    private JwtRequest jwtRequest;
    private SaveUserDTO saveUserDTO;
    private JwtResponse jwtResponse;
    private User user;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        jwtRequest = new JwtRequest();
        jwtRequest.setUsername("testuser");
        jwtRequest.setPassword("password");

        saveUserDTO = new SaveUserDTO();
        saveUserDTO.setUsername("testuser");
        saveUserDTO.setEmail("test@example.com");
        saveUserDTO.setPassword("password");
        saveUserDTO.setConfirmPassword("password");
        saveUserDTO.setName("Test User");
        saveUserDTO.setPhone("1234567890");

        jwtResponse = new JwtResponse("testtoken");

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@example.com");
    }

    @Test
    void createAuthToken_ValidRequest_ReturnsJwtResponse() {
        // Given
        when(bindingResult.hasErrors()).thenReturn(false);
        when(authService.createAuthToken(any(JwtRequest.class))).thenReturn(jwtResponse);

        // When
        ResponseEntity<?> response = authController.createAuthToken(jwtRequest, bindingResult);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(jwtResponse, response.getBody());

        verify(authService, times(1)).createAuthToken(any(JwtRequest.class));
    }

    @Test
    void createAuthToken_InvalidRequest_ThrowsAuthException() {
        // Given
        when(bindingResult.hasErrors()).thenReturn(true);
        //doThrow(new AuthException("Validation error")).when(bindingResult).getAllErrors();

        // When & Then
        UserException exception = assertThrows(UserException.class,
                () -> authController.createAuthToken(jwtRequest, bindingResult));

        //assertEquals("Validation error", exception.getMessage());
        verify(bindingResult, times(1)).hasErrors();
        verify(authService, never()).createAuthToken(any(JwtRequest.class));
    }

    @Test
    void createNewUser_ValidRequest_ReturnsUser() {
        // Given
        when(bindingResult.hasErrors()).thenReturn(false);
        when(authService.createNewUser(any(SaveUserDTO.class))).thenReturn(user);
        when(modelMapper.map(user, UserDTO.class)).thenReturn(userDTO);

        // When
        ResponseEntity<?> response = authController.createNewUser(saveUserDTO, bindingResult);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(userDTO, response.getBody());

        verify(authService, times(1)).createNewUser(any(SaveUserDTO.class));
    }

    @Test
    void createNewUser_InvalidRequest_ThrowsUserException() {
        // Given
        when(bindingResult.hasErrors()).thenReturn(true);
        //doThrow(new AuthException("Validation error")).when(bindingResult).getAllErrors();

        // When & Then
        UserException exception = assertThrows(UserException.class,
                () -> authController.createNewUser(saveUserDTO, bindingResult));

        //assertEquals("Validation error", exception.getMessage());
        verify(bindingResult, times(1)).hasErrors();
        verify(authService, never()).createNewUser(any(SaveUserDTO.class));
    }
}