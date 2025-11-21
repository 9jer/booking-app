package com.example.userservice.controllers;

import com.example.userservice.dto.JwtRequest;
import com.example.userservice.dto.SaveUserDTO;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.models.Role;
import com.example.userservice.models.User;
import com.example.userservice.services.AuthService;
import com.example.userservice.util.AuthException;
import com.example.userservice.util.ErrorResponse;
import com.example.userservice.util.ErrorsUtil;
import com.example.userservice.util.UserException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("${application.endpoint.auth.root}")
public class AuthController {
    private final AuthService authService;
    private final Logger LOG = LoggerFactory.getLogger(AuthController.class);
    private final ModelMapper modelMapper;

    @PostMapping(path = "${application.endpoint.auth.sign-in}")
    public ResponseEntity<?> createAuthToken(@RequestBody @Valid JwtRequest authRequest, BindingResult bindingResult) {

        LOG.info("Received auth request: {}", authRequest);

        if (bindingResult.hasErrors()) {
            ErrorsUtil.returnAllErrors(bindingResult);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(authService.createAuthToken(authRequest));
    }

    @PostMapping(path = "${application.endpoint.auth.sign-up}")
    public ResponseEntity<?> createNewUser(@RequestBody @Valid SaveUserDTO saveUserDTO, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            ErrorsUtil.returnAllErrors(bindingResult);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(convertUserToUserDTO(authService.createNewUser(saveUserDTO)));
    }

    private UserDTO convertUserToUserDTO(User user) {
        UserDTO userDTO = modelMapper.map(user, UserDTO.class);
        if (user.getRoles() != null) {
            userDTO.setRoles(user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toList()));
        }
        return userDTO;
    }

    @ExceptionHandler
    private ResponseEntity<ErrorResponse> handleException(UserException e) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                        e.getMessage()));
    }

    @ExceptionHandler
    private ResponseEntity<ErrorResponse> handleException(AuthException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(),
                        e.getMessage()));
    }

}
