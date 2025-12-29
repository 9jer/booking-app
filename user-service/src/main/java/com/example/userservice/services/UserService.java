package com.example.userservice.services;

import com.example.userservice.dto.UpdateUserDTO;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserService {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    UserDTO createNewUser(User user);
    Page<UserDTO> findAll(Pageable pageable);
    UserDTO getUserById(Long id);
    UserDTO getUserByUsername(String username);
    UserDTO updateUser(UpdateUserDTO updateUserDTO, String token);
    UserDTO assignOwnerRole(Long userId);
    Boolean existsById(Long id);
    void deleteUserById(Long id, String token);
}
