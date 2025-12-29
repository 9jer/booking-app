package com.example.userservice.services;

import com.example.userservice.dto.JwtRequest;
import com.example.userservice.dto.JwtResponse;
import com.example.userservice.dto.SaveUserDTO;
import com.example.userservice.dto.UserDTO;

public interface AuthService {

    JwtResponse createAuthToken(JwtRequest authRequest);
    UserDTO createNewUser(SaveUserDTO saveUserDTO);

}
