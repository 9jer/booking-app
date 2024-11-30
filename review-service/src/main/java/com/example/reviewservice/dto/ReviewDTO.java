package com.example.reviewservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewDTO {
    @NotNull(message = "Property id should not be empty!")
    private Long propertyId;

    @NotNull(message = "User id should not be empty!")
    private Long userId;

    @NotNull(message = "Rating should not be empty!")
    @Min(value = 1, message = "Rating should be greater than 1 and less than 5")
    @Max(value = 5, message = "Rating should be greater than 1 and less than 5")
    private Integer rating;

    @NotEmpty(message = "Comment should not be empty!")
    private String comment;
}
