package com.example.propertyservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Set;

@Getter
@Setter
public class GetPropertyDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull(message = "Owner id should not be empty!")
    private Long ownerId;

    @NotEmpty(message = "Title should not be empty!")
    private String title;

    @NotEmpty(message = "Description should not be empty!")
    private String description;

    @NotEmpty(message = "Location should not be empty!")
    private String location;

    private BigDecimal averageRating;

    @NotNull(message = "Price per night should not be empty!")
    private BigDecimal pricePerNight;

    @NotNull(message = "Capacity should not be empty!")
    private Integer capacity;

    private Set<PropertyFeatureDTO> features;
}
