package com.example.userservice.services;

import com.example.userservice.dto.UpdateUserDTO;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.mapper.UserMapper;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private UserMapper userMapper;

    @Mock
    private JwtTokenUtils jwtTokenUtils;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserDTO userDTO;
    private UpdateUserDTO updateUserDTO;
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

        updateUserDTO = new UpdateUserDTO();
        updateUserDTO.setUsername("testuser");
        updateUserDTO.setEmail("test@example.com");
        updateUserDTO.setFirstName("Test");
        updateUserDTO.setLastName("User");
        updateUserDTO.setPhone("1234567890");

        guestRole = new Role();
        guestRole.setId(1);
        guestRole.setName("ROLE_GUEST");

        ownerRole = new Role();
        ownerRole.setId(2);
        ownerRole.setName("ROLE_OWNER");
    }

    @Test
    void findByUsername_ShouldReturnOptionalUser() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.findByUsername("testuser");

        // Then
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    void findByEmail_ShouldReturnOptionalUser() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.findByEmail("test@example.com");

        // Then
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    void findAll_ShouldReturnPagedUserDTOs() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(user));

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toUserDTO(any(User.class))).thenReturn(userDTO);

        // Act
        Page<UserDTO> result = userService.findAll(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("testuser", result.getContent().get(0).getUsername());
        verify(userRepository, times(1)).findAll(pageable);
    }

    @Test
    void getUserById_WhenUserFound_ShouldReturnUserDTO() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toUserDTO(user)).thenReturn(userDTO);

        // Act
        UserDTO result = userService.getUserById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void getUserById_WhenUserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        UserException exception = assertThrows(UserException.class,
                () -> userService.getUserById(99L));
        assertEquals("User 99 not found", exception.getMessage());
    }

    @Test
    void getUserByUsername_WhenUserFound_ShouldReturnUserDTO() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userMapper.toUserDTO(user)).thenReturn(userDTO);

        // Act
        UserDTO result = userService.getUserByUsername("testuser");

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    void getUserByUsername_WhenUserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // When & Then
        UserException exception = assertThrows(UserException.class,
                () -> userService.getUserByUsername("unknown"));
        assertEquals("User not found with username: unknown", exception.getMessage());
    }

    @Test
    void existsById_ShouldReturnBoolean() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);

        // Act
        Boolean result = userService.existsById(1L);

        // Then
        assertTrue(result);
        verify(userRepository, times(1)).existsById(1L);
    }

    @Test
    @Transactional
    void createNewUser_ValidUser_CreatesAndReturnsUser() {
        // Given
        User inputUser = new User();
        inputUser.setUsername("newuser");
        inputUser.setPassword("password");

        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        when(userMapper.toUserDTO(any(User.class))).thenReturn(userDTO);

        // When
        UserDTO result = userService.createNewUser(inputUser);

        // Then
        assertNotNull(result);
        verify(userRepository, times(1)).save(inputUser);
    }

    @Test
    void updateUser_Owner_UpdatesUser() {
        User updatedUserFromDTO = new User();
        updatedUserFromDTO.setUsername("newuser");
        updatedUserFromDTO.setPassword("newpassword");

        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        when(userMapper.toUser(updateUserDTO)).thenReturn(updatedUserFromDTO);

        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserDTO(user)).thenReturn(userDTO);

        UserDTO result = userService.updateUser(updateUserDTO, token);

        assertNotNull(result);
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_EmailAlreadyTaken_ThrowsException() {
        // Given
        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setEmail("taken@example.com");

        updateUserDTO.setEmail("taken@example.com");

        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(anotherUser));

        // When & Then
        UserException exception = assertThrows(UserException.class,
                () -> userService.updateUser(updateUserDTO, token));
        assertEquals("Email already taken", exception.getMessage());
    }

    @Test
    @Transactional
    void assignOwnerRole_ValidUser_AddsOwnerRole() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleService.getOwnerRole()).thenReturn(ownerRole);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserDTO(user)).thenReturn(userDTO);

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