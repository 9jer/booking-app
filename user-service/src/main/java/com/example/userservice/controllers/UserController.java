package com.example.userservice.controllers;

import com.example.userservice.dto.UpdateUserDTO;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.security.CustomUserDetailsService;
import com.example.userservice.services.UserService;
import com.example.userservice.util.ErrorResponse;
import com.example.userservice.util.ErrorsUtil;
import com.example.userservice.util.JwtTokenUtils;
import com.example.userservice.util.UserException;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("${application.endpoint.users.root}")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final JwtTokenUtils jwtTokenUtils;
    private final CustomUserDetailsService userDetailsService;

    @GetMapping
    public ResponseEntity<Page<UserDTO>> getAllUsers(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(userService.findAll(pageable));
    }

    @PatchMapping
    public ResponseEntity<UserDTO> updateUserInfo(@Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader,
                                              @RequestBody @Valid UpdateUserDTO updateUserDTO, BindingResult bindingResult) {

        if(bindingResult.hasErrors()) {
            ErrorsUtil.returnAllErrors(bindingResult);
        }

        String cleanToken = authorizationHeader.startsWith("Bearer ") ? authorizationHeader.substring(7) : authorizationHeader;

        UserDTO updatedUser = userService.updateUser(updateUserDTO, cleanToken);

        UserDetails userDetails = userDetailsService.loadUserByUsername(updatedUser.getUsername());
        String newToken = jwtTokenUtils.generateToken(userDetails);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + newToken)
                .body(updatedUser);
    }

    @DeleteMapping(path = "${application.endpoint.users.id}")
    public ResponseEntity<HttpStatus> deleteUser(@Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader,
                                                 @PathVariable("id") Long id) {
        String jwtToken = authorizationHeader.replace("Bearer ", "");
        userService.deleteUserById(id, jwtToken);

        return ResponseEntity.ok(HttpStatus.OK);
    }

    @GetMapping("${application.endpoint.users.id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable("id") Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(userService.getUserById(id));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getUserProfile(Principal principal) {
        return ResponseEntity.ok(userService.getUserByUsername(principal.getName()));
    }

    @PostMapping(path = "${application.endpoint.users.assign-owner}")
    public ResponseEntity<UserDTO> assignOwnerRole(@PathVariable("id") Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(userService.assignOwnerRole(id));
    }

    @GetMapping(path = "${application.endpoint.users.exists}")
    public ResponseEntity<Boolean> userExists(@PathVariable("id") Long id) {
        return ResponseEntity.ok(userService.existsById(id));
    }

    @ExceptionHandler
    private ResponseEntity<ErrorResponse> handleException(UserException e) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                        e.getMessage()));
    }
}