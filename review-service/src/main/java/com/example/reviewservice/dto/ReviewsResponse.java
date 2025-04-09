package com.example.reviewservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class ReviewsResponse {
    private List<GetReviewDTO> reviews;
}
