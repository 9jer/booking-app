package com.example.userservice.services;

import com.example.userservice.models.Role;

public interface RoleService {
    Role getGuestRole();
    Role getOwnerRole();
    Role findByName(String name);
}
