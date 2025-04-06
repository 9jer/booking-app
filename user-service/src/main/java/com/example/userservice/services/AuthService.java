package com.example.userservice.services;

import com.example.userservice.dto.JwtRequest;
import com.example.userservice.dto.JwtResponse;
import com.example.userservice.dto.SaveUserDTO;
import com.example.userservice.models.User;

public interface AuthService {

    JwtResponse createAuthToken(JwtRequest authRequest);
    User createNewUser(SaveUserDTO saveUserDTO);

}
