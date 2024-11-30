package com.example.userservice.services;

import com.example.userservice.models.Role;
import com.example.userservice.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService {
    private final RoleRepository roleRepository;

    public Role getGuestRole() {
        return roleRepository.findByName("ROLE_GUEST").get();
    }

    public Role getOwnerRole() {
        return roleRepository.findByName("ROLE_OWNER")
                .orElseThrow(() -> new RuntimeException("Role OWNER not found"));
    }

}
