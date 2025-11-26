package com.example.userservice.services;

import com.example.userservice.dto.SaveUserDTO;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.models.Role;
import com.example.userservice.models.User;
import com.example.userservice.repositories.UserRepository;
import com.example.userservice.util.JwtTokenUtils;
import com.example.userservice.util.UserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private JwtTokenUtils jwtTokenUtils;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserDTO userDTO;
    private SaveUserDTO saveUserDTO;
    private Role guestRole;
    private Role ownerRole;
    private String token = "valid-token";

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setRoles(new ArrayList<>());

        userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@example.com");

        saveUserDTO = new SaveUserDTO();
        saveUserDTO.setUsername("testuser");
        saveUserDTO.setEmail("test@example.com");
        saveUserDTO.setPassword("password");
        saveUserDTO.setConfirmPassword("password");
        saveUserDTO.setName("Test User");
        saveUserDTO.setPhone("1234567890");

        guestRole = new Role();
        guestRole.setId(1);
        guestRole.setName("ROLE_GUEST");

        ownerRole = new Role();
        ownerRole.setId(2);
        ownerRole.setName("ROLE_OWNER");
    }

    @Test
    @Transactional
    void createNewUser_ValidUser_CreatesAndReturnsUser() {
        // Given
        User inputUser = new User();
        inputUser.setUsername("newuser");
        inputUser.setPassword("password");

        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(roleService.getGuestRole()).thenReturn(guestRole);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        when(modelMapper.map(any(User.class), eq(UserDTO.class))).thenReturn(userDTO);

        // When
        UserDTO result = userService.createNewUser(inputUser);

        // Then
        assertNotNull(result);
        verify(userRepository, times(1)).save(inputUser);
    }

    @Test
    void updateUserById_Owner_UpdatesUser() {
        User updatedUserFromDTO = new User();
        updatedUserFromDTO.setUsername("newuser");
        updatedUserFromDTO.setPassword("newpassword");

        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        when(modelMapper.map(saveUserDTO, User.class)).thenReturn(updatedUserFromDTO);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedNewPass");

        when(userRepository.save(user)).thenReturn(user);
        when(modelMapper.map(user, UserDTO.class)).thenReturn(userDTO);

        UserDTO result = userService.updateUserById(1L, saveUserDTO, token);

        assertNotNull(result);
        verify(userRepository).save(user);
    }

    @Test
    @Transactional
    void assignOwnerRole_ValidUser_AddsOwnerRole() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleService.getOwnerRole()).thenReturn(ownerRole);
        when(userRepository.save(user)).thenReturn(user);
        when(modelMapper.map(user, UserDTO.class)).thenReturn(userDTO);

        // When
        UserDTO result = userService.assignOwnerRole(1L);

        // Then
        assertNotNull(result);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @Transactional
    void assignOwnerRole_UserAlreadyHasRole_ThrowsException() {
        // Given
        User userWithRole = new User();
        userWithRole.setId(1L);
        userWithRole.setRoles(new ArrayList<>(List.of(guestRole, ownerRole)));

        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithRole));
        when(roleService.getOwnerRole()).thenReturn(ownerRole);

        // When & Then
        UserException exception = assertThrows(UserException.class, () -> userService.assignOwnerRole(1L));
        assertEquals("User already has OWNER role", exception.getMessage());

        verify(userRepository, never()).save(any());
    }

    @Test
    @Transactional
    void deleteUserById_Owner_DeletesUser() {
        // Given
        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);
        when(userRepository.existsById(1L)).thenReturn(true);

        doNothing().when(userRepository).deleteById(1L);

        // When
        userService.deleteUserById(1L, token);

        // Then
        verify(userRepository, times(1)).deleteById(1L);
    }
}