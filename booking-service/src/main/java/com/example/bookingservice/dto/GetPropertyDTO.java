package com.example.bookingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GetPropertyDTO {

    private Long id;
    private Long ownerId;
    private String title;
    private String description;
    private String location;
    private BigDecimal averageRating;
    private BigDecimal pricePerNight;
    private Integer capacity;
    private Set<PropertyFeatureDTO> features;
}
