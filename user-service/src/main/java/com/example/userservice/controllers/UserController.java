package com.example.userservice.controllers;

import com.example.userservice.dto.SaveUserDTO;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.dto.UsersResponse;
import com.example.userservice.models.User;
import com.example.userservice.services.UserService;
import com.example.userservice.util.ErrorResponse;
import com.example.userservice.util.ErrorsUtil;
import com.example.userservice.util.UserException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final ModelMapper modelMapper;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public UsersResponse getAllUsers() {
        return new UsersResponse(userService.findAll().stream().map(this::convertUserToUserDTO).toList());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<HttpStatus> updateUser(@PathVariable("id") Long id, @RequestBody @Valid SaveUserDTO saveUserDTO, BindingResult bindingResult) {

        if(bindingResult.hasErrors()) {
            ErrorsUtil.returnAllErrors(bindingResult);
        }

        userService.updateUserById(id, saveUserDTO);

        return ResponseEntity.ok(HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteUser(@PathVariable("id") Long id) {
        userService.deleteUserById(id);

        return ResponseEntity.ok(HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public UserDTO getUserById(@PathVariable("id") Long id) {
        return convertUserToUserDTO(userService.getUserById(id));
    }

    @PostMapping("/{id}/assign-owner")
    public ResponseEntity<UserDTO> assignOwnerRole(@PathVariable("id") Long id) {
        User updatedUser = userService.assignOwnerRole(id);
        return ResponseEntity.ok(convertUserToUserDTO(updatedUser));
    }

    @GetMapping("/{id}/exists")
    public Boolean userExists(@PathVariable Long id) {
        return userService.existsById(id);
    }

    @GetMapping("/info")
    public String userData(Principal principal){
        return principal.getName();
    }

    private UserDTO convertUserToUserDTO(User user){
        return modelMapper.map(user, UserDTO.class);
    }

    private User convertUserDTOToUser(SaveUserDTO saveUserDTO){
        return modelMapper.map(saveUserDTO, User.class);
    }

    @ExceptionHandler
    private ResponseEntity<ErrorResponse> handleException(UserException e) {
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                e.getMessage()), HttpStatus.BAD_REQUEST);
    }
}
