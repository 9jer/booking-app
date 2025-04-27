package com.example.reviewservice.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Reviews")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @Column(name = "property_id")
    @NotNull(message = "Property id should not be empty!")
    private Long propertyId;

    @Column(name = "user_id")
    @NotNull(message = "User id should not be empty!")
    private Long userId;

    @Column(name = "rating")
    @NotNull(message = "Rating should not be empty!")
    @Min(value = 1)
    @Max(value = 5)
    private Integer rating;

    @Column(name = "comment", length = 1024)
    @NotEmpty(message = "Comment should not be empty!")
    private String comment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
