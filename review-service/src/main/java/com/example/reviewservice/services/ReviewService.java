package com.example.reviewservice.services;

import com.example.reviewservice.dto.GetReviewDTO;
import com.example.reviewservice.models.Review;

import java.util.List;

public interface ReviewService {
    List<GetReviewDTO> getReviewsByPropertyId(Long propertyId);
    GetReviewDTO saveReview(Review review, String token);
    GetReviewDTO updateReview(Review review, String token);
    GetReviewDTO deleteReview(Long reviewId, String token);
}
