package com.example.userservice.controllers;

import com.example.userservice.dto.JwtRequest;
import com.example.userservice.dto.SaveUserDTO;
import com.example.userservice.services.AuthService;
import com.example.userservice.util.AuthException;
import com.example.userservice.util.ErrorResponse;
import com.example.userservice.util.ErrorsUtil;
import com.example.userservice.util.UserException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/sign-in")
    public ResponseEntity<?> createAuthToken(@RequestBody @Valid JwtRequest authRequest, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            ErrorsUtil.returnAllErrors(bindingResult);
        }

        return ResponseEntity.ok(authService.createAuthToken(authRequest));
    }

    @PostMapping("/sign-up")
    public ResponseEntity<?> createNewUser(@RequestBody @Valid SaveUserDTO saveUserDTO, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            ErrorsUtil.returnAllErrors(bindingResult);
        }

        return ResponseEntity.ok(authService.createNewUser(saveUserDTO));
    }

    @ExceptionHandler
    private ResponseEntity<ErrorResponse> handleException(UserException e) {
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    private ResponseEntity<ErrorResponse> handleException(AuthException e) {
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(),
                e.getMessage()), HttpStatus.UNAUTHORIZED);
    }

}
