package com.example.userservice.services;

import com.example.userservice.controllers.AuthController;
import com.example.userservice.dto.JwtRequest;
import com.example.userservice.dto.JwtResponse;
import com.example.userservice.dto.SaveUserDTO;
import com.example.userservice.models.User;
import com.example.userservice.security.CustomUserDetails;
import com.example.userservice.security.CustomUserDetailsService;
import com.example.userservice.util.AuthException;
import com.example.userservice.util.ErrorsUtil;
import com.example.userservice.util.JwtTokenUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {
    private final UserService userService;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtTokenUtils jwtTokenUtils;
    private final AuthenticationManager authenticationManager;
    private final ModelMapper modelMapper;
    private final Logger LOG = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Override
    public JwtResponse createAuthToken(JwtRequest authRequest) {
        LOG.info("Attempting to authenticate user: {}", authRequest.getUsername());
        try{
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));
        } catch (BadCredentialsException e) {
            LOG.error("Authentication failed for user: {}", authRequest.getUsername(), e);
            System.out.println("Bad credentials: " + authRequest.getUsername());
            throw new AuthException("Incorrect login or password!");
        }

        CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService
                .loadUserByUsername(authRequest.getUsername());

        String token = jwtTokenUtils.generateToken(userDetails);

        return new JwtResponse(token);
    }

    @Override
    @Transactional
    public User createNewUser(SaveUserDTO saveUserDTO) {

        ErrorsUtil.validateInputUserData(saveUserDTO,
                userService.findByUsername(saveUserDTO.getUsername()),
                userService.findByEmail(saveUserDTO.getEmail()));

        User user = convertRegistrationUserDTOToUser(saveUserDTO);

        return userService.createNewUser(user);
    }

    private User convertRegistrationUserDTOToUser(SaveUserDTO saveUserDTO) {
        return modelMapper.map(saveUserDTO, User.class);
    }
}
