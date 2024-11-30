package com.example.reviewservice.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class ReviewErrorResponse {
    private String message;
    private Long timestamp;
}
