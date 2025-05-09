package com.example.userservice.services;

import com.example.userservice.dto.SaveUserDTO;
import com.example.userservice.models.Role;
import com.example.userservice.models.User;
import com.example.userservice.repositories.UserRepository;
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

import java.time.LocalDateTime;
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

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private SaveUserDTO saveUserDTO;
    private Role guestRole;
    private Role ownerRole;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setName("Test User");
        user.setPhone("1234567890");
        user.setCreatedAt(LocalDateTime.now());

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
    void findByUsername_UserExists_ReturnsUser() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.findByUsername("testuser");

        // Then
        assertTrue(result.isPresent());
        assertEquals(user, result.get());
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    void findByEmail_UserExists_ReturnsUser() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.findByEmail("test@example.com");

        // Then
        assertTrue(result.isPresent());
        assertEquals(user, result.get());
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    @Transactional
    void createNewUser_ValidUser_CreatesAndReturnsUser() {
        // Given
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPassword("password");

        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(roleService.getGuestRole()).thenReturn(guestRole);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        // When
        User result = userService.createNewUser(newUser);

        // Then
        assertNotNull(result.getId());
        assertEquals("encodedPassword", result.getPassword());
        assertTrue(result.getRoles().contains(guestRole));
        assertNotNull(result.getCreatedAt());
        verify(userRepository, times(1)).save(newUser);
    }

    @Test
    void findAll_ReturnsAllUsers() {
        // Given
        when(userRepository.findAll()).thenReturn(List.of(user));

        // When
        List<User> result = userService.findAll();

        // Then
        assertEquals(1, result.size());
        assertEquals(user, result.get(0));
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getUserById_UserExists_ReturnsUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // When
        User result = userService.getUserById(1L);

        // Then
        assertEquals(user, result);
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void getUserById_UserNotExists_ThrowsException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        UserException exception = assertThrows(UserException.class,
                () -> userService.getUserById(1L));

        assertEquals("User 1 not found", exception.getMessage());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void updateUserById_ValidData_UpdatesUser() {
        SaveUserDTO updatedDto = new SaveUserDTO();
        updatedDto.setUsername("newuser");
        updatedDto.setEmail("new@example.com");
        updatedDto.setPassword("newpassword");
        updatedDto.setConfirmPassword("newpassword");
        updatedDto.setName("New Name");
        updatedDto.setPhone("9876543210");

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("olduser");

        User updatedUser = new User();
        updatedUser.setUsername("newuser");
        updatedUser.setPassword("newpassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(modelMapper.map(updatedDto, User.class)).thenReturn(updatedUser);
        when(passwordEncoder.encode("newpassword")).thenReturn("encodedNewPass");
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        User result = userService.updateUserById(1L, updatedDto);

        assertEquals("newuser", result.getUsername());
        assertEquals("encodedNewPass", result.getPassword());
        assertNotNull(result.getUpdatedAt());
        verify(userRepository).save(existingUser);
    }



    @Test
    @Transactional
    void assignOwnerRole_ValidUser_AddsOwnerRole() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setRoles(new ArrayList<>(List.of(guestRole)));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleService.getOwnerRole()).thenReturn(ownerRole);
        when(userRepository.save(user)).thenReturn(user);

        // When
        User result = userService.assignOwnerRole(1L);

        // Then
        assertEquals(2, result.getRoles().size());
        assertTrue(result.getRoles().contains(ownerRole));
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void existsById_UserExists_ReturnsTrue() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);

        // When
        Boolean result = userService.existsById(1L);

        // Then
        assertTrue(result);
        verify(userRepository, times(1)).existsById(1L);
    }

    @Test
    @Transactional
    void deleteUserById_ValidId_DeletesUser() {
        // Given
        doNothing().when(userRepository).deleteById(1L);

        // When
        userService.deleteUserById(1L);

        // Then
        verify(userRepository, times(1)).deleteById(1L);
    }
}