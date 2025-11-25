package com.example.userservice.services;

import com.example.userservice.dto.SaveUserDTO;
import com.example.userservice.models.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    User createNewUser(User user);
    List<User> findAll();
    User getUserById(Long id);
    User updateUserById(Long id, SaveUserDTO updatedUser, String token);
    User assignOwnerRole(Long userId);
    Boolean existsById(Long id);
    void deleteUserById(Long id, String token);
}
