package com.example.userservice.services;

import com.example.userservice.models.Role;
import com.example.userservice.repositories.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role guestRole;
    private Role ownerRole;

    @BeforeEach
    void setUp() {
        guestRole = new Role();
        guestRole.setId(1);
        guestRole.setName("ROLE_GUEST");

        ownerRole = new Role();
        ownerRole.setId(2);
        ownerRole.setName("ROLE_OWNER");
    }

    @Test
    void getGuestRole_WhenRoleExists_ReturnsRole() {
        // Given
        when(roleRepository.findByName("ROLE_GUEST")).thenReturn(Optional.of(guestRole));

        // When
        Role result = roleService.getGuestRole();

        // Then
        assertNotNull(result);
        assertEquals("ROLE_GUEST", result.getName());
        verify(roleRepository, times(1)).findByName("ROLE_GUEST");
    }

    @Test
    void getGuestRole_WhenRoleNotExists_ThrowsException() {
        // Given
        when(roleRepository.findByName("ROLE_GUEST")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> roleService.getGuestRole());
        verify(roleRepository, times(1)).findByName("ROLE_GUEST");
    }

    @Test
    void getOwnerRole_WhenRoleExists_ReturnsRole() {
        // Given
        when(roleRepository.findByName("ROLE_OWNER")).thenReturn(Optional.of(ownerRole));

        // When
        Role result = roleService.getOwnerRole();

        // Then
        assertNotNull(result);
        assertEquals("ROLE_OWNER", result.getName());
        verify(roleRepository, times(1)).findByName("ROLE_OWNER");
    }

    @Test
    void getOwnerRole_WhenRoleNotExists_ThrowsException() {
        // Given
        when(roleRepository.findByName("ROLE_OWNER")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> roleService.getOwnerRole());

        assertEquals("Role OWNER not found", exception.getMessage());
        verify(roleRepository, times(1)).findByName("ROLE_OWNER");
    }
}