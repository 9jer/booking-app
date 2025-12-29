package com.example.reviewservice.services;

import com.example.reviewservice.dto.GetReviewDTO;
import com.example.reviewservice.models.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    Page<GetReviewDTO> getReviewsByPropertyId(Long propertyId, Pageable pageable);
    GetReviewDTO saveReview(Review review, String token);
    GetReviewDTO updateReview(Review review, String token);
    GetReviewDTO deleteReview(Long reviewId, String token);
}
