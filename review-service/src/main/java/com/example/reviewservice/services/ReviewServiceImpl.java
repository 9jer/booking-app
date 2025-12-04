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
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
@Slf4j
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final PropertyClient propertyClient;
    private final UserClient userClient;
    private final BookingClient bookingClient;
    private final JwtTokenUtils jwtTokenUtils;
    private final RatingEventProducer ratingEventProducer;
    private final ModelMapper modelMapper;
    private final TransactionTemplate transactionTemplate;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             PropertyClient propertyClient,
                             UserClient userClient,
                             BookingClient bookingClient,
                             JwtTokenUtils jwtTokenUtils,
                             RatingEventProducer ratingEventProducer,
                             ModelMapper modelMapper,
                             PlatformTransactionManager transactionManager) {
        this.reviewRepository = reviewRepository;
        this.propertyClient = propertyClient;
        this.userClient = userClient;
        this.bookingClient = bookingClient;
        this.jwtTokenUtils = jwtTokenUtils;
        this.ratingEventProducer = ratingEventProducer;
        this.modelMapper = modelMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public Page<GetReviewDTO> getReviewsByPropertyId(Long propertyId, Pageable pageable) {
        return reviewRepository.findByPropertyId(propertyId, pageable)
                .map(this::convertToGetReviewDTO);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
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

        return transactionTemplate.execute(status -> {
            enrichReview(review);
            Review savedReview = reviewRepository.save(review);

            executeAfterCommit(() -> {
                try {
                    ratingEventProducer.sendRatingUpdatedEvent(savedReview.getPropertyId());
                } catch (Exception e) {
                    log.error("Failed to send rating update event for property " + savedReview.getPropertyId(), e);
                }
            });

            return convertToGetReviewDTO(savedReview);
        });
    }

    @Override
    @Transactional
    public GetReviewDTO updateReview(Review review, String token) {

        Long userId = jwtTokenUtils.getUserId(token);
        List<String> roles = jwtTokenUtils.getRoles(token);

        Review existingReview = reviewRepository.findById(review.getId()).orElseThrow(
                () -> new ReviewException("Review with id " + review.getId() + " not found."));

        if (!roles.contains("ROLE_ADMIN") && !existingReview.getUserId().equals(userId)) {
            throw new ReviewException("You can only update your own reviews");
        }

        if(review.getPropertyId() != null && !existingReview.getPropertyId().equals(review.getPropertyId())) {
            throw new ReviewException("Changing the property of a review is not allowed.");
        }

        existingReview.setRating(review.getRating());
        existingReview.setComment(review.getComment());

        enrichUpdatedReview(existingReview);

        Review updatedReview = reviewRepository.save(existingReview);

        Long propertyId = existingReview.getPropertyId();
        executeAfterCommit(() -> {
            try {
                ratingEventProducer.sendRatingUpdatedEvent(propertyId);
            } catch (Exception e) {
                log.error("Failed to send rating update event for property " + propertyId, e);
            }
        });

        return convertToGetReviewDTO(updatedReview);
    }

    @Override
    @Transactional
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

        executeAfterCommit(() -> {
            try {
                ratingEventProducer.sendRatingUpdatedEvent(propertyId);
            } catch (Exception e) {
                log.error("Failed to send rating update event for property " + propertyId, e);
            }
        });

        return convertToGetReviewDTO(existingReview);
    }

    protected void executeAfterCommit(Runnable runnable) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runnable.run();
            }
        });
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