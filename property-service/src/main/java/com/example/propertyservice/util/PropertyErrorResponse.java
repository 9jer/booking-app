package com.example.propertyservice.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class PropertyErrorResponse {
    private String message;
    private Long timestamp;
}
