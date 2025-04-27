package com.example.reviewservice.services;

import com.example.reviewservice.client.BookingClient;
import com.example.reviewservice.client.PropertyClient;
import com.example.reviewservice.client.UserClient;
import com.example.reviewservice.event.RatingEventProducer;
import com.example.reviewservice.models.Review;
import com.example.reviewservice.repositories.ReviewRepository;
import com.example.reviewservice.util.JwtTokenUtils;
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
    private final JwtTokenUtils jwtTokenUtils;
    private final RatingEventProducer ratingEventProducer;

    @Override
    public List<Review> getReviewsByPropertyId(Long propertyId) {
        return reviewRepository.findByPropertyId(propertyId);
    }

    @Override
    @Transactional
    public Review saveReview(Review review, String token) {
        review.setId(null);
        Boolean propertyExists = propertyClient.propertyExists(review.getPropertyId());

        review.setUserId(jwtTokenUtils.getUserId(token));
        Boolean userExists = userClient.userExists(review.getUserId());

        Boolean wasBooked = bookingClient.wasBooked(review.getPropertyId(), review.getUserId());

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

        boolean alreadyReview = reviewRepository.existsByUserIdAndPropertyId(
                review.getUserId(), review.getPropertyId()
        );

        if (alreadyReview) {
            throw new ReviewException("You have already reviewed this property!");
        }

        enrichReview(review);
        Review savedReview = reviewRepository.save(review);

        ratingEventProducer.sendRatingUpdatedEvent(review.getPropertyId());

        return savedReview;
    }

    private void enrichReview(Review review) {
        review.setCreatedAt(LocalDateTime.now());
    }
}
