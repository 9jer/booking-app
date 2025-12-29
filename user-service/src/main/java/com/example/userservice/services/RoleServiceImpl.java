package com.example.userservice.services;

import com.example.userservice.models.Role;
import com.example.userservice.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;

    @Override
    @Cacheable(value = "roles", key = "'guest'")
    public Role getGuestRole() {
        return roleRepository.findByName("ROLE_GUEST")
                .orElseThrow(() -> new RuntimeException("Role GUEST not found"));
    }

    @Override
    @Cacheable(value = "roles", key = "'owner'")
    public Role getOwnerRole() {
        return roleRepository.findByName("ROLE_OWNER")
                .orElseThrow(() -> new RuntimeException("Role OWNER not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Role findByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Role not found: " + name));
    }
}