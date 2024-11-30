package com.example.reviewservice.services;

import com.example.reviewservice.client.PropertyClient;
import com.example.reviewservice.client.UserClient;
import com.example.reviewservice.models.Review;
import com.example.reviewservice.repositories.ReviewRepository;
import com.example.reviewservice.util.ReviewException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final PropertyClient propertyClient;
    private final UserClient userClient;

    public ReviewService(ReviewRepository reviewRepository, PropertyClient propertyClient, UserClient userClient) {
        this.reviewRepository = reviewRepository;
        this.propertyClient = propertyClient;
        this.userClient = userClient;
    }

    public List<Review> getReviewsByPropertyId(Long propertyId) {
        return reviewRepository.findByPropertyId(propertyId);
    }

    @Transactional
    public Review saveReview(Review review, String jwtToken) {
        Boolean propertyExists = propertyClient
                .checkPropertyExists(review.getPropertyId(), jwtToken).block();

        Boolean userExists = userClient.checkUserExists(review.getUserId(), jwtToken).block();

        if (propertyExists == null || !propertyExists) {
            throw new ReviewException("Property with id " + review.getPropertyId()
                    + " not found.");
        }

        if (userExists == null || !userExists) {
            throw new ReviewException("User with id " + review.getUserId()
                    + " not found.");
        }

        enrichReview(review);
        return reviewRepository.save(review);
    }

    private void enrichReview(Review review) {
        review.setCreatedAt(LocalDateTime.now());
    }
}