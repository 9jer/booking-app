package com.example.userservice.controllers;

import com.example.userservice.dto.SaveUserDTO;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.dto.UsersResponse;
import com.example.userservice.models.Role;
import com.example.userservice.models.User;
import com.example.userservice.services.UserService;
import com.example.userservice.util.ErrorResponse;
import com.example.userservice.util.ErrorsUtil;
import com.example.userservice.util.UserException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${application.endpoint.users.root}")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<UsersResponse> getAllUsers() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UsersResponse(userService.findAll().stream().map(this::convertUserToUserDTO).toList()));
    }

    @PatchMapping(path = "${application.endpoint.users.id}")
    public ResponseEntity<UserDTO> updateUser(@RequestHeader("Authorization") String authorizationHeader,
                                              @PathVariable("id") Long id, @RequestBody @Valid SaveUserDTO saveUserDTO, BindingResult bindingResult) {

        if(bindingResult.hasErrors()) {
            ErrorsUtil.returnAllErrors(bindingResult);
        }

        String jwtToken = authorizationHeader.replace("Bearer ", "");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(convertUserToUserDTO(userService.updateUserById(id, saveUserDTO, jwtToken)));
    }

    @DeleteMapping(path = "${application.endpoint.users.id}")
    public ResponseEntity<HttpStatus> deleteUser(@RequestHeader("Authorization") String authorizationHeader,
                                                 @PathVariable("id") Long id) {
        String jwtToken = authorizationHeader.replace("Bearer ", "");
        userService.deleteUserById(id, jwtToken);

        return ResponseEntity.ok(HttpStatus.OK);
    }

    @GetMapping("${application.endpoint.users.id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable("id") Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(convertUserToUserDTO(userService.getUserById(id)));
    }

    @PostMapping(path = "${application.endpoint.users.assign-owner}")
    public ResponseEntity<UserDTO> assignOwnerRole(@PathVariable("id") Long id) {
        User updatedUser = userService.assignOwnerRole(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(convertUserToUserDTO(updatedUser));
    }

    @GetMapping(path = "${application.endpoint.users.exists}")
    public ResponseEntity<Boolean> userExists(@PathVariable("id") Long id) {
        return ResponseEntity.ok(userService.existsById(id));
    }

    @GetMapping(path = "${application.endpoint.users.info}")
    public String userData(Principal principal){
        return principal.getName();
    }

    private UserDTO convertUserToUserDTO(User user){
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
}
