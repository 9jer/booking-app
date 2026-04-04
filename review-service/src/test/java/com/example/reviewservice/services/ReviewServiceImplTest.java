package com.example.reviewservice.services;

import com.example.reviewservice.client.*;
import com.example.reviewservice.dto.GetReviewDTO;
import com.example.reviewservice.event.RatingEventProducer;
import com.example.reviewservice.mapper.ReviewMapper;
import com.example.reviewservice.models.Review;
import com.example.reviewservice.repositories.ReviewRepository;
import com.example.reviewservice.util.JwtTokenUtils;
import com.example.reviewservice.util.ReviewException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    @Spy
    private ReviewServiceImpl reviewService;

    private Review review;
    private GetReviewDTO getReviewDTO;
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

        getReviewDTO = new GetReviewDTO();
        getReviewDTO.setId(1L);
        getReviewDTO.setRating(5);

        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @Test
    void getReviewsByPropertyId_ReturnsReviewsList() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(reviewRepository.findByPropertyId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));
        when(reviewMapper.toGetReviewDTO(review)).thenReturn(getReviewDTO);

        // When
        Page<GetReviewDTO> result = reviewService.getReviewsByPropertyId(1L, pageable);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals(getReviewDTO, result.getContent().get(0));
        verify(reviewRepository, times(1)).findByPropertyId(1L, pageable);
    }

    @Test
    @Transactional
    void saveReview_ValidReview_SavesAndReturnsReview() {
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(reviewService).executeAfterCommit(any());

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
        when(reviewMapper.toGetReviewDTO(review)).thenReturn(getReviewDTO);

        // When
        GetReviewDTO result = reviewService.saveReview(newReview, validToken);

        // Then
        assertNotNull(result);
        assertEquals(getReviewDTO.getId(), result.getId());

        verify(reviewRepository, times(1)).save(any(Review.class));
        verify(ratingEventProducer, times(1)).sendRatingUpdatedEvent(1L);
    }

    @Test
    void saveReview_UserDidNotBook_ThrowsException() {
        // Given
        Review newReview = new Review();
        newReview.setPropertyId(1L);
        newReview.setUserId(1L);

        when(propertyClient.propertyExists(1L)).thenReturn(true);
        when(jwtTokenUtils.getUserId(validToken)).thenReturn(1L);
        when(userClient.userExists(1L)).thenReturn(true);

        when(bookingClient.wasBooked(1L, 1L)).thenReturn(false);

        // When & Then
        ReviewException exception = assertThrows(ReviewException.class,
                () -> reviewService.saveReview(newReview, validToken));

        assertTrue(exception.getMessage().contains("until you've lived there"));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void saveReview_WhenUserDoesNotExist_ShouldThrowException() {
        // Given
        Review review = new Review();
        review.setUserId(1L);
        review.setPropertyId(100L);
        String token = "Bearer token";

        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);

        org.mockito.Mockito.lenient().when(propertyClient.propertyExists(100L)).thenReturn(true);
        org.mockito.Mockito.lenient().when(bookingClient.wasBooked(100L, 1L)).thenReturn(true);

        when(userClient.userExists(1L)).thenReturn(false);

        // When & Then
        ReviewException exception = assertThrows(ReviewException.class,
                () -> reviewService.saveReview(review, token));
        assertEquals("User with id 1 not found.", exception.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void saveReview_WhenKafkaFails_ShouldCatchException() {
        // Given
        Review review = new Review();
        review.setUserId(1L);
        review.setPropertyId(100L);
        review.setRating(5);
        String token = "Bearer token";

        com.example.reviewservice.dto.GetReviewDTO dummyDto = new com.example.reviewservice.dto.GetReviewDTO();

        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);
        when(propertyClient.propertyExists(100L)).thenReturn(true);
        when(bookingClient.wasBooked(100L, 1L)).thenReturn(true);
        when(userClient.userExists(1L)).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenReturn(review);
        when(reviewMapper.toGetReviewDTO(any(Review.class))).thenReturn(dummyDto);

        doThrow(new RuntimeException("Kafka is down")).when(ratingEventProducer)
                .sendRatingUpdatedEvent(anyLong());

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertDoesNotThrow(() -> reviewService.saveReview(review, token));

            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
            }

            verify(ratingEventProducer, times(1)).sendRatingUpdatedEvent(anyLong());
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    @Transactional
    void updateReview_Owner_UpdatesReview() {
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(reviewService).executeAfterCommit(any());

        // Given
        Review updatedReviewInput = new Review();
        updatedReviewInput.setId(1L);
        updatedReviewInput.setPropertyId(1L);
        updatedReviewInput.setRating(4);
        updatedReviewInput.setComment("Updated comment");

        Review existingReview = new Review();
        existingReview.setId(1L);
        existingReview.setPropertyId(1L);
        existingReview.setUserId(1L);
        existingReview.setRating(5);
        existingReview.setComment("Original comment");
        existingReview.setCreatedAt(LocalDateTime.now());

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(existingReview));
        when(jwtTokenUtils.getUserId(validToken)).thenReturn(1L);
        when(reviewRepository.save(any(Review.class))).thenReturn(existingReview);
        when(reviewMapper.toGetReviewDTO(existingReview)).thenReturn(getReviewDTO);

        // When
        GetReviewDTO result = reviewService.updateReview(updatedReviewInput, validToken);

        // Then
        assertNotNull(result);
        assertEquals(getReviewDTO, result);

        verify(reviewRepository, times(1)).save(existingReview);
    }

    @Test
    void updateReview_WhenReviewNotFound_ShouldThrowException() {
        // Given
        Review review = new Review();
        review.setId(99L);
        String token = "Bearer token";

        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        ReviewException exception = assertThrows(ReviewException.class,
                () -> reviewService.updateReview(review, token));
        assertEquals("Review with id 99 not found.", exception.getMessage());
    }

    @Test
    void updateReview_WhenUserNotOwnerAndNotAdmin_ShouldThrowException() {
        // Given
        Review review = new Review();
        review.setId(1L);
        String token = "Bearer token";

        Review existingReview = new Review();
        existingReview.setId(1L);
        existingReview.setUserId(2L);

        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);
        when(jwtTokenUtils.getRoles(token)).thenReturn(List.of("ROLE_USER"));
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(existingReview));

        // When & Then
        ReviewException exception = assertThrows(ReviewException.class,
                () -> reviewService.updateReview(review, token));
        assertEquals("You can only update your own reviews", exception.getMessage());
    }

    @Test
    void updateReview_WhenPropertyIdChanged_ShouldThrowException() {
        // Given
        Review review = new Review();
        review.setId(1L);
        review.setPropertyId(200L);
        String token = "Bearer token";

        Review existingReview = new Review();
        existingReview.setId(1L);
        existingReview.setUserId(1L);
        existingReview.setPropertyId(100L);

        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);
        when(jwtTokenUtils.getRoles(token)).thenReturn(List.of("ROLE_USER"));
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(existingReview));

        // When & Then
        ReviewException exception = assertThrows(ReviewException.class,
                () -> reviewService.updateReview(review, token));
        assertEquals("Changing the property of a review is not allowed.", exception.getMessage());
    }

    @Test
    void updateReview_WhenKafkaFails_ShouldCatchException() {
        // Given
        Review review = new Review();
        review.setId(1L);
        review.setPropertyId(100L);
        review.setRating(4);
        String token = "Bearer token";

        Review existingReview = new Review();
        existingReview.setId(1L);
        existingReview.setUserId(1L);
        existingReview.setPropertyId(100L);

        com.example.reviewservice.dto.GetReviewDTO dummyDto = new com.example.reviewservice.dto.GetReviewDTO();

        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);
        when(jwtTokenUtils.getRoles(token)).thenReturn(List.of("ROLE_USER"));
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(existingReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(existingReview);
        when(reviewMapper.toGetReviewDTO(any(Review.class))).thenReturn(dummyDto);

        doThrow(new RuntimeException("Kafka error")).when(ratingEventProducer)
                .sendRatingUpdatedEvent(anyLong());

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertDoesNotThrow(() -> reviewService.updateReview(review, token));

            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
            }

            verify(ratingEventProducer, times(1)).sendRatingUpdatedEvent(anyLong());
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    @Transactional
    void deleteReview_Owner_DeletesReview() {
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(reviewService).executeAfterCommit(any());

        // Given
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(jwtTokenUtils.getUserId(validToken)).thenReturn(1L);
        doNothing().when(reviewRepository).delete(review);
        when(reviewMapper.toGetReviewDTO(review)).thenReturn(getReviewDTO);

        // When
        GetReviewDTO result = reviewService.deleteReview(1L, validToken);

        // Then
        assertNotNull(result);
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

    @Test
    void deleteReview_WhenReviewNotFound_ShouldThrowException() {
        // Given
        String token = "Bearer token";
        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);
        when(jwtTokenUtils.getRoles(token)).thenReturn(List.of("ROLE_USER"));
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        ReviewException exception = assertThrows(ReviewException.class,
                () -> reviewService.deleteReview(99L, token));
        assertEquals("Review with id 99 not found.", exception.getMessage());
    }

    @Test
    void deleteReview_WhenKafkaFails_ShouldCatchException() {
        // Given
        String token = "Bearer token";
        Review existingReview = new Review();
        existingReview.setId(1L);
        existingReview.setUserId(1L);
        existingReview.setPropertyId(100L);

        com.example.reviewservice.dto.GetReviewDTO dummyDto = new com.example.reviewservice.dto.GetReviewDTO();

        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);
        when(jwtTokenUtils.getRoles(token)).thenReturn(List.of("ROLE_USER"));
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(existingReview));
        when(reviewMapper.toGetReviewDTO(any(Review.class))).thenReturn(dummyDto);

        doThrow(new RuntimeException("Kafka error")).when(ratingEventProducer)
                .sendRatingUpdatedEvent(anyLong());

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertDoesNotThrow(() -> reviewService.deleteReview(1L, token));

            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
            }

            verify(ratingEventProducer, times(1)).sendRatingUpdatedEvent(anyLong());
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }
}