package com.example.reviewservice.services;

import com.example.reviewservice.client.BookingClient;
import com.example.reviewservice.client.PropertyClient;
import com.example.reviewservice.client.UserClient;
import com.example.reviewservice.dto.GetReviewDTO;
import com.example.reviewservice.event.RatingEventProducer;
import com.example.reviewservice.models.Review;
import com.example.reviewservice.repositories.ReviewRepository;
import com.example.reviewservice.util.JwtTokenUtils;
import com.example.reviewservice.util.ReviewException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
    private final ModelMapper modelMapper;

    @Override
    @Cacheable(value = "reviewsByProperty", key = "#propertyId")
    public List<GetReviewDTO> getReviewsByPropertyId(Long propertyId) {
        return reviewRepository.findByPropertyId(propertyId).stream()
                .map(this::convertToGetReviewDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "reviewsByProperty", key = "#result.propertyId")
    public GetReviewDTO saveReview(Review review, String token) {
        review.setId(null);
        Boolean propertyExists = propertyClient.propertyExists(review.getPropertyId());

        review.setUserId(jwtTokenUtils.getUserId(token));
        Boolean userExists = userClient.userExists(review.getUserId());

        if (Boolean.FALSE.equals(propertyExists)) {
            throw new ReviewException("Property with id " + review.getPropertyId() + " not found.");
        }
        if (Boolean.FALSE.equals(userExists)) {
            throw new ReviewException("User with id " + review.getUserId() + " not found.");
        }

        Boolean wasBooked = bookingClient.wasBooked(review.getPropertyId(), review.getUserId());
        if (Boolean.FALSE.equals(wasBooked)) {
            throw new ReviewException("You can't leave a review on a place " + review.getPropertyId() +
                    " until you've lived there.");
        }

        enrichReview(review);
        Review savedReview = reviewRepository.save(review);

        ratingEventProducer.sendRatingUpdatedEvent(review.getPropertyId());

        return convertToGetReviewDTO(savedReview);
    }

    @Override
    @Transactional
    @CacheEvict(value = "reviewsByProperty", key = "#result.propertyId")
    public GetReviewDTO updateReview(Review review, String token) {

        Long userId = jwtTokenUtils.getUserId(token);
        List<String> roles = jwtTokenUtils.getRoles(token);

        Review existingReview = reviewRepository.findById(review.getId()).orElseThrow(
                () -> new ReviewException("Review with id " + review.getId() + " not found."));

        if (!roles.contains("ROLE_ADMIN") && !existingReview.getUserId().equals(userId)) {
            throw new ReviewException("You can only update your own reviews");
        }

        review.setPropertyId(existingReview.getPropertyId());

        existingReview.setRating(review.getRating());
        existingReview.setComment(review.getComment());
        enrichUpdatedReview(existingReview);

        Review updatedReview = reviewRepository.save(existingReview);

        ratingEventProducer.sendRatingUpdatedEvent(existingReview.getPropertyId());

        return convertToGetReviewDTO(updatedReview);
    }

    @Override
    @Transactional
    @CacheEvict(value = "reviewsByProperty", key = "#result.propertyId")
    public GetReviewDTO deleteReview(Long reviewId, String token) {
        Long userId = jwtTokenUtils.getUserId(token);
        List<String> roles = jwtTokenUtils.getRoles(token);

        Review existingReview = reviewRepository.findById(reviewId).orElseThrow(
                () -> new ReviewException("Review with id " + reviewId + " not found."));

        if (!roles.contains("ROLE_ADMIN") && !existingReview.getUserId().equals(userId)) {
            throw new ReviewException("You can only delete your own reviews");
        }

        Long propertyId = existingReview.getPropertyId();
        reviewRepository.delete(existingReview);

        ratingEventProducer.sendRatingUpdatedEvent(propertyId);

        return convertToGetReviewDTO(existingReview);
    }

    private void enrichUpdatedReview(Review existingReview) {
        existingReview.setUpdatedAt(LocalDateTime.now());
    }

    private void enrichReview(Review review) {
        review.setCreatedAt(LocalDateTime.now());
    }

    private GetReviewDTO convertToGetReviewDTO(Review review) {
        return modelMapper.map(review, GetReviewDTO.class);
    }
}