package com.example.reviewservice.services;

import com.example.reviewservice.client.BookingClient;
import com.example.reviewservice.client.PropertyClient;
import com.example.reviewservice.client.UserClient;
import com.example.reviewservice.models.Review;
import com.example.reviewservice.repositories.ReviewRepository;
import com.example.reviewservice.util.ReviewException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final PropertyClient propertyClient;
    private final UserClient userClient;
    private final BookingClient bookingClient;

    @Override
    public List<Review> getReviewsByPropertyId(Long propertyId) {
        return reviewRepository.findByPropertyId(propertyId);
    }

    @Override
    @Transactional
    public Review saveReview(Review review, String jwtToken) {
        Boolean propertyExists = propertyClient
                .checkPropertyExists(review.getPropertyId(), jwtToken).block();

        Boolean userExists = userClient.checkUserExists(review.getUserId(), jwtToken).block();

        Boolean wasBooked = bookingClient.checkIfItWasBooked(review.getPropertyId(), review.getUserId(), jwtToken).block();

        if (propertyExists == null || !propertyExists) {
            throw new ReviewException("Property with id " + review.getPropertyId()
                    + " not found.");
        }

        if (userExists == null || !userExists) {
            throw new ReviewException("User with id " + review.getUserId()
                    + " not found.");
        }

        if (wasBooked == null || !wasBooked) {
            throw new ReviewException("You can't leave a review on a place " + review.getPropertyId() +
                    " until you've lived there.");
        }

        enrichReview(review);
        return reviewRepository.save(review);
    }

    private void enrichReview(Review review) {
        review.setCreatedAt(LocalDateTime.now());
    }
}
