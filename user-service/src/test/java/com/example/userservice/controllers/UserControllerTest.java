package com.example.userservice.controllers;

import com.example.userservice.dto.SaveUserDTO;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.dto.UsersResponse;
import com.example.userservice.models.User;
import com.example.userservice.services.UserService;
import com.example.userservice.util.ErrorResponse;
import com.example.userservice.util.UserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import java.security.Principal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private User user;
    private UserDTO userDTO;
    private SaveUserDTO saveUserDTO;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setName("Test User");
        user.setPhone("1234567890");

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
    void getAllUsers_ReturnsUsersResponse() {
        // Given
        when(userService.findAll()).thenReturn(Collections.singletonList(user));
        when(modelMapper.map(any(User.class), eq(UserDTO.class))).thenReturn(userDTO);

        // When
        ResponseEntity<UsersResponse> response = userController.getAllUsers();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getUsers().size());
        assertEquals(userDTO, response.getBody().getUsers().get(0));

        verify(userService, times(1)).findAll();
        verify(modelMapper, times(1)).map(any(User.class), eq(UserDTO.class));
    }

    @Test
    void getUserById_UserExists_ReturnsUserDTO() {
        // Given
        when(userService.getUserById(1L)).thenReturn(user);
        when(modelMapper.map(any(User.class), eq(UserDTO.class))).thenReturn(userDTO);

        // When
        ResponseEntity<UserDTO> response = userController.getUserById(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(userDTO, response.getBody());

        verify(userService, times(1)).getUserById(1L);
        verify(modelMapper, times(1)).map(any(User.class), eq(UserDTO.class));
    }

    @Test
    void getUserById_UserNotExists_ThrowsUserException() {
        // Given
        when(userService.getUserById(1L)).thenThrow(new UserException("User not found"));

        // When & Then
        UserException exception = assertThrows(UserException.class,
                () -> userController.getUserById(1L));

        assertEquals("User not found", exception.getMessage());
        verify(userService, times(1)).getUserById(1L);
    }

    @Test
    void updateUser_ValidRequest_ReturnsUpdatedUser() {
        // Given
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userService.updateUserById(anyLong(), any(SaveUserDTO.class))).thenReturn(user);
        when(modelMapper.map(any(User.class), eq(UserDTO.class))).thenReturn(userDTO);

        // When
        ResponseEntity<UserDTO> response = userController.updateUser(1L, saveUserDTO, bindingResult);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(userDTO, response.getBody());

        verify(userService, times(1)).updateUserById(1L, saveUserDTO);
        verify(modelMapper, times(1)).map(any(User.class), eq(UserDTO.class));
    }

    @Test
    void updateUser_InvalidRequest_ThrowsUserException() {
        // Given
        when(bindingResult.hasErrors()).thenReturn(true);

        // When & Then
        assertThrows(UserException.class,
                () -> userController.updateUser(1L, saveUserDTO, bindingResult));

        verify(bindingResult, times(1)).hasErrors();
        verify(userService, never()).updateUserById(anyLong(), any(SaveUserDTO.class));
    }

    @Test
    void deleteUser_ValidId_ReturnsOkStatus() {
        // Given
        doNothing().when(userService).deleteUserById(1L);

        // When
        ResponseEntity<HttpStatus> response = userController.deleteUser(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userService, times(1)).deleteUserById(1L);
    }

    @Test
    void assignOwnerRole_ValidId_ReturnsUserDTO() {
        // Given
        when(userService.assignOwnerRole(1L)).thenReturn(user);
        when(modelMapper.map(any(User.class), eq(UserDTO.class))).thenReturn(userDTO);

        // When
        ResponseEntity<UserDTO> response = userController.assignOwnerRole(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(userDTO, response.getBody());

        verify(userService, times(1)).assignOwnerRole(1L);
        verify(modelMapper, times(1)).map(any(User.class), eq(UserDTO.class));
    }

    @Test
    void userExists_UserExists_ReturnsTrue() {
        // Given
        when(userService.existsById(1L)).thenReturn(true);

        // When
        ResponseEntity<Boolean> response = userController.userExists(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody());
        verify(userService, times(1)).existsById(1L);
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