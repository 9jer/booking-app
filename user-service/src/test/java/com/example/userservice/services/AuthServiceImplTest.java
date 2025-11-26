package com.example.userservice.services;

import com.example.userservice.dto.JwtRequest;
import com.example.userservice.dto.JwtResponse;
import com.example.userservice.dto.SaveUserDTO;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.models.User;
import com.example.userservice.security.CustomUserDetails;
import com.example.userservice.security.CustomUserDetailsService;
import com.example.userservice.util.AuthException;
import com.example.userservice.util.JwtTokenUtils;
import com.example.userservice.util.UserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private JwtTokenUtils jwtTokenUtils;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private JwtRequest jwtRequest;
    private SaveUserDTO saveUserDTO;
    private User user;
    private UserDTO userDTO;
    private CustomUserDetails userDetails;

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

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setUsername("testuser");

        userDetails = new CustomUserDetails(user);
    }

    @Test
    void createAuthToken_ValidCredentials_ReturnsJwtResponse() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(customUserDetailsService.loadUserByUsername("testuser"))
                .thenReturn(userDetails);
        when(jwtTokenUtils.generateToken(userDetails))
                .thenReturn("test-token");

        // When
        JwtResponse response = authService.createAuthToken(jwtRequest);

        // Then
        assertNotNull(response);
        assertEquals("test-token", response.getToken());
        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void createAuthToken_InvalidCredentials_ThrowsAuthException() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When & Then
        AuthException exception = assertThrows(AuthException.class,
                () -> authService.createAuthToken(jwtRequest));

        assertEquals("Incorrect login or password!", exception.getMessage());
    }

    @Test
    @Transactional
    void createNewUser_ValidData_CreatesUser() {
        // Given
        when(userService.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(modelMapper.map(saveUserDTO, User.class)).thenReturn(user);

        when(userService.createNewUser(user)).thenReturn(userDTO);

        // When
        UserDTO result = authService.createNewUser(saveUserDTO);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userService, times(1)).createNewUser(user);
    }

    @Test
    @Transactional
    void createNewUser_ExistingUsername_ThrowsException() {
        // Given
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(user));

        // When & Then
        UserException exception = assertThrows(UserException.class,
                () -> authService.createNewUser(saveUserDTO));

        assertEquals("User with username testuser already exist", exception.getMessage());
        verify(userService, never()).createNewUser(any(User.class));
    }

    @Test
    @Transactional
    void createNewUser_ExistingEmail_ThrowsException() {
        // Given
        when(userService.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // When & Then
        UserException exception = assertThrows(UserException.class,
                () -> authService.createNewUser(saveUserDTO));

        assertEquals("Email test@example.com address already in use!", exception.getMessage());
        verify(userService, never()).createNewUser(any(User.class));
    }

}