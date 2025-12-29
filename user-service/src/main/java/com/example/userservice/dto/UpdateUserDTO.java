package com.example.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserDTO {
    @NotEmpty(message = "Username should not be empty!")
    private String username;

    @NotEmpty(message = "Email should not be empty!")
    @Email(message = "Email is incorrect!")
    private String email;

    @NotEmpty(message = "First name should not be empty!")
    private String firstName;

    @NotEmpty(message = "Last name should not be empty!")
    private String lastName;

    @NotEmpty(message = "Phone should not be empty!")
    private String phone;
}