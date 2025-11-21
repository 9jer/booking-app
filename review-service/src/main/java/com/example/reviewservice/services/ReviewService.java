package com.example.reviewservice.services;

import com.example.reviewservice.models.Review;

import java.util.List;

public interface ReviewService {
    List<Review> getReviewsByPropertyId(Long propertyId);
    Review saveReview(Review review, String token);
    Review updateReview(Review review, String token);
    Review deleteReview(Long reviewId, String token);
}
