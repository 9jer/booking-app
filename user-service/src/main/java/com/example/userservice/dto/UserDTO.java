package com.example.userservice.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotEmpty(message = "Username should not be empty!")
    private String username;

    @NotEmpty(message = "Email should not be empty!")
    private String email;

    @NotEmpty(message = "First name should not be empty!")
    private String firstName;

    @NotEmpty(message = "Last name should not be empty!")
    private String lastName;

    @NotEmpty(message = "Phone should not be empty!")
    private String phone;

    private List<String> roles;
}
