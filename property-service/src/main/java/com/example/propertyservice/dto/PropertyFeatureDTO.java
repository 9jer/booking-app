package com.example.propertyservice.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertyFeatureDTO {

    @NotEmpty(message = "Feature name should not be empty!")
    private String name;
}
