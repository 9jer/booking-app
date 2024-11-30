package com.example.propertyservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class PropertiesResponse {
    private List<PropertyDTO> properties;
}
