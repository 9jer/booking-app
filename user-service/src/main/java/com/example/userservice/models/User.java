package com.example.userservice.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Collection;

@Entity
@Table(name = "Users")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "username")
    @NotEmpty(message = "Username should not be empty!")
    private String username;

    @Column(name = "email")
    @NotEmpty(message = "Email should not be empty!")
    private String email;

    @Column(name = "password")
    @NotEmpty(message = "Password should not be empty!")
    private String password;

    @Column(name = "name")
    @NotEmpty(message = "Name should not be empty!")
    private String name;

    @Column(name = "phone")
    @NotEmpty(message = "Phone should not be empty!")
    private String phone;

    @ManyToMany
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Collection<Role> roles;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
