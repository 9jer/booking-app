package com.example.reviewservice.controllers;

import com.example.reviewservice.dto.GetReviewDTO;
import com.example.reviewservice.dto.ReviewDTO;
import com.example.reviewservice.dto.ReviewsResponse;
import com.example.reviewservice.models.Review;
import com.example.reviewservice.services.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private ReviewController reviewController;

    private Review review;
    private ReviewDTO reviewDTO;
    private GetReviewDTO getReviewDTO;

    @BeforeEach
    void setUp() {
        review = new Review();
        review.setId(1L);
        review.setPropertyId(1L);
        review.setUserId(1L);
        review.setRating(5);
        review.setComment("Great property!");

        reviewDTO = new ReviewDTO();
        reviewDTO.setPropertyId(1L);
        reviewDTO.setRating(5);
        reviewDTO.setComment("Great property!");

        getReviewDTO = new GetReviewDTO();
        getReviewDTO.setId(1L);
        getReviewDTO.setPropertyId(1L);
        getReviewDTO.setUserId(1L);
        getReviewDTO.setRating(5);
        getReviewDTO.setComment("Great property!");
    }

    @Test
    void getReviewsByPropertyId_ReturnsReviewsResponse() {
        // Given
        when(reviewService.getReviewsByPropertyId(1L)).thenReturn(Collections.singletonList(getReviewDTO));

        // When
        ResponseEntity<ReviewsResponse> response = reviewController.getReviewsByPropertyId(1L);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getReviews().size());
        assertEquals(getReviewDTO, response.getBody().getReviews().get(0));

        verify(reviewService, times(1)).getReviewsByPropertyId(1L);
    }

    @Test
    void createReview_ValidRequest_ReturnsGetReviewDTO() {
        // Given
        String authHeader = "Bearer token";
        when(bindingResult.hasErrors()).thenReturn(false);
        when(modelMapper.map(any(ReviewDTO.class), eq(Review.class))).thenReturn(review);
        when(reviewService.saveReview(any(Review.class), anyString())).thenReturn(getReviewDTO);

        // When
        ResponseEntity<GetReviewDTO> response = reviewController.createReview(authHeader, reviewDTO, bindingResult);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(getReviewDTO, response.getBody());

        verify(reviewService, times(1)).saveReview(any(Review.class), anyString());
        verify(modelMapper, times(1)).map(any(ReviewDTO.class), eq(Review.class));
    }

    @Test
    void updateReview_ValidRequest_ReturnsUpdatedGetReviewDTO() {
        // Given
        String authHeader = "Bearer token";
        when(bindingResult.hasErrors()).thenReturn(false);
        when(modelMapper.map(any(ReviewDTO.class), eq(Review.class))).thenReturn(review);
        when(reviewService.updateReview(any(Review.class), anyString())).thenReturn(getReviewDTO);

        // When
        ResponseEntity<GetReviewDTO> response = reviewController.updateReview(authHeader, 1L, reviewDTO, bindingResult);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(getReviewDTO, response.getBody());
        assertEquals(1L, response.getBody().getId());

        verify(reviewService, times(1)).updateReview(any(Review.class), anyString());
        verify(modelMapper, times(1)).map(any(ReviewDTO.class), eq(Review.class));
    }

    @Test
    void deleteReview_ValidId_ReturnsNoContent() {
        // Given
        String authHeader = "Bearer token";
        when(reviewService.deleteReview(anyLong(), anyString())).thenReturn(getReviewDTO);

        // When
        ResponseEntity<Void> response = reviewController.deleteReview(authHeader, 1L);

        // Then
        assertEquals(204, response.getStatusCodeValue());
        verify(reviewService, times(1)).deleteReview(1L, "token");
    }
}