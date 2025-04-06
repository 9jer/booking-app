package com.example.reviewservice.services;

import com.example.reviewservice.models.Review;

import java.util.List;

public interface ReviewService {
    List<Review> getReviewsByPropertyId(Long propertyId);
    Review saveReview(Review review, String jwtToken);
}
