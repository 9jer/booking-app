package com.example.reviewservice.services;

import com.example.reviewservice.client.*;
import com.example.reviewservice.event.RatingEventProducer;
import com.example.reviewservice.models.Review;
import com.example.reviewservice.repositories.ReviewRepository;
import com.example.reviewservice.util.JwtTokenUtils;
import com.example.reviewservice.util.ReviewException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private PropertyClient propertyClient;

    @Mock
    private UserClient userClient;

    @Mock
    private BookingClient bookingClient;

    @Mock
    private JwtTokenUtils jwtTokenUtils;

    @Mock
    private RatingEventProducer ratingEventProducer;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private Review review;
    private String validToken = "valid-token";

    @BeforeEach
    void setUp() {
        review = new Review();
        review.setId(1L);
        review.setPropertyId(1L);
        review.setUserId(1L);
        review.setRating(5);
        review.setComment("Great property!");
        review.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void getReviewsByPropertyId_ReturnsReviewsList() {
        // Given
        when(reviewRepository.findByPropertyId(1L)).thenReturn(List.of(review));

        // When
        List<Review> result = reviewService.getReviewsByPropertyId(1L);

        // Then
        assertEquals(1, result.size());
        assertEquals(review, result.get(0));
        verify(reviewRepository, times(1)).findByPropertyId(1L);
    }

    @Test
    @Transactional
    void saveReview_ValidReview_SavesAndReturnsReview() {
        // Given
        Review newReview = new Review();
        newReview.setPropertyId(1L);
        newReview.setRating(5);
        newReview.setComment("Test comment");

        when(propertyClient.propertyExists(1L)).thenReturn(true);
        when(jwtTokenUtils.getUserId(validToken)).thenReturn(1L);
        when(userClient.userExists(1L)).thenReturn(true);
        when(bookingClient.wasBooked(1L, 1L)).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        // When
        Review result = reviewService.saveReview(newReview, validToken);

        // Then
        assertNotNull(result);
        assertNotNull(result.getCreatedAt());
        verify(reviewRepository, times(1)).save(any(Review.class));
        verify(ratingEventProducer, times(1)).sendRatingUpdatedEvent(1L);
    }

    @Test
    @Transactional
    void saveReview_PropertyNotExists_ThrowsException() {
        // Given
        when(propertyClient.propertyExists(1L)).thenReturn(false);

        // When & Then
        ReviewException exception = assertThrows(ReviewException.class,
                () -> reviewService.saveReview(review, validToken));

        assertEquals("Property with id 1 not found.", exception.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @Transactional
    void saveReview_UserNotExists_ThrowsException() {
        // Given
        when(propertyClient.propertyExists(1L)).thenReturn(true);
        when(jwtTokenUtils.getUserId(validToken)).thenReturn(1L);
        when(userClient.userExists(1L)).thenReturn(false);

        // When & Then
        ReviewException exception = assertThrows(ReviewException.class,
                () -> reviewService.saveReview(review, validToken));

        assertEquals("User with id 1 not found.", exception.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @Transactional
    void saveReview_NotBookedProperty_ThrowsException() {
        // Given
        when(propertyClient.propertyExists(1L)).thenReturn(true);
        when(jwtTokenUtils.getUserId(validToken)).thenReturn(1L);
        when(userClient.userExists(1L)).thenReturn(true);
        when(bookingClient.wasBooked(1L, 1L)).thenReturn(false);

        // When & Then
        ReviewException exception = assertThrows(ReviewException.class,
                () -> reviewService.saveReview(review, validToken));

        assertEquals("You can't leave a review on a place 1 until you've lived there.",
                exception.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @Transactional
    void updateReview_Owner_UpdatesReview() {
        // Given
        Review updatedReview = new Review();
        updatedReview.setId(1L);
        updatedReview.setPropertyId(1L);
        updatedReview.setRating(4);
        updatedReview.setComment("Updated comment");

        Review existingReview = new Review();
        existingReview.setId(1L);
        existingReview.setPropertyId(1L);
        existingReview.setUserId(1L);
        existingReview.setRating(5);
        existingReview.setComment("Original comment");
        existingReview.setCreatedAt(LocalDateTime.now());

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(existingReview));
        when(jwtTokenUtils.getUserId(validToken)).thenReturn(1L);
        when(propertyClient.propertyExists(1L)).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenReturn(existingReview);

        // When
        Review result = reviewService.updateReview(updatedReview, validToken);

        // Then
        assertNotNull(result);
        assertEquals(4, result.getRating());
        verify(reviewRepository, times(1)).save(existingReview);
    }

    @Test
    @Transactional
    void updateReview_NotOwner_ThrowsException() {
        // Given
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(jwtTokenUtils.getUserId(validToken)).thenReturn(2L);
        when(jwtTokenUtils.getRoles(validToken)).thenReturn(List.of("ROLE_USER"));

        // When & Then
        ReviewException exception = assertThrows(ReviewException.class,
                () -> reviewService.updateReview(review, validToken));

        assertEquals("You can only update your own reviews", exception.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @Transactional
    void deleteReview_Owner_DeletesReview() {
        // Given
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(jwtTokenUtils.getUserId(validToken)).thenReturn(1L);
        doNothing().when(reviewRepository).delete(review);

        // When
        reviewService.deleteReview(1L, validToken);

        // Then
        verify(reviewRepository, times(1)).delete(review);
        verify(ratingEventProducer, times(1)).sendRatingUpdatedEvent(1L);
    }

    @Test
    @Transactional
    void deleteReview_NotOwner_ThrowsException() {
        // Given
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(jwtTokenUtils.getUserId(validToken)).thenReturn(2L);
        when(jwtTokenUtils.getRoles(validToken)).thenReturn(List.of("ROLE_USER"));

        // When & Then
        ReviewException exception = assertThrows(ReviewException.class,
                () -> reviewService.deleteReview(1L, validToken));

        assertEquals("You can only delete your own reviews", exception.getMessage());
        verify(reviewRepository, never()).delete(any(Review.class));
    }
}