package com.example.reviewservice.controllers;

import com.example.reviewservice.dto.GetReviewDTO;
import com.example.reviewservice.dto.ReviewDTO;
import com.example.reviewservice.models.Review;
import com.example.reviewservice.services.ReviewService;
import com.example.reviewservice.util.JwtTokenUtils;
import com.example.reviewservice.util.ReviewException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerIT {

    private static final String ROOT_ENDPOINT = "/api/v1/reviews";
    private static final String REVIEWS_BY_PROPERTY_ENDPOINT = ROOT_ENDPOINT + "/property/{id}";
    private static final String ID_ENDPOINT = ROOT_ENDPOINT + "/{id}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private JwtTokenUtils jwtTokenUtils;

    private Review testReview;
    private ReviewDTO testReviewDTO;
    private GetReviewDTO testGetReviewDTO;
    private String validToken = "valid.token.here";

    @BeforeEach
    void setUp() {
        testReview = new Review();
        testReview.setId(1L);
        testReview.setPropertyId(1L);
        testReview.setUserId(1L);
        testReview.setRating(5);
        testReview.setComment("Great place!");
        testReview.setCreatedAt(LocalDateTime.now());

        testReviewDTO = new ReviewDTO();
        testReviewDTO.setPropertyId(1L);
        testReviewDTO.setRating(5);
        testReviewDTO.setComment("Great place!");

        testGetReviewDTO = new GetReviewDTO();
        testGetReviewDTO.setId(1L);
        testGetReviewDTO.setPropertyId(1L);
        testGetReviewDTO.setUserId(1L);
        testGetReviewDTO.setRating(5);
        testGetReviewDTO.setComment("Great place!");

        Mockito.when(jwtTokenUtils.getUserId(anyString())).thenReturn(1L);
        Mockito.when(jwtTokenUtils.getRoles(anyString())).thenReturn(List.of("ROLE_USER"));
    }

    @Test
    void getReviewsByPropertyId_ShouldReturnReviews() throws Exception {
        Mockito.when(reviewService.getReviewsByPropertyId(anyLong()))
                .thenReturn(List.of(testReview));

        mockMvc.perform(get(REVIEWS_BY_PROPERTY_ENDPOINT, 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviews[0].id").value(testReview.getId()))
                .andExpect(jsonPath("$.reviews[0].propertyId").value(testReview.getPropertyId()));
    }

    @Test
    void createReview_WithValidData_ShouldReturnCreatedReview() throws Exception {
        Mockito.when(reviewService.saveReview(any(Review.class), anyString()))
                .thenReturn(testReview);

        mockMvc.perform(post(ROOT_ENDPOINT)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testReviewDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testReview.getId()))
                .andExpect(jsonPath("$.propertyId").value(testReview.getPropertyId()));
    }

    @Test
    void createReview_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        ReviewDTO invalidReviewDTO = new ReviewDTO();
        invalidReviewDTO.setRating(6);

        mockMvc.perform(post(ROOT_ENDPOINT)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReviewDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateReview_WithValidData_ShouldReturnUpdatedReview() throws Exception {
        Mockito.when(reviewService.updateReview(any(Review.class), anyString()))
                .thenReturn(testReview);

        mockMvc.perform(put(ID_ENDPOINT, 1L)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testReviewDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testReview.getId()));
    }

    @Test
    void updateReview_WithInvalidId_ShouldReturnNotFound() throws Exception {
        Mockito.when(reviewService.updateReview(any(Review.class), anyString()))
                .thenThrow(new ReviewException("Review not found"));

        mockMvc.perform(put(ID_ENDPOINT, 999L)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testReviewDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Review not found"));
    }

    @Test
    void deleteReview_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete(ID_ENDPOINT, 1L)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReview_WithInvalidId_ShouldReturnNotFound() throws Exception {
        Mockito.doThrow(new ReviewException("Review not found"))
                .when(reviewService).deleteReview(anyLong(), anyString());

        mockMvc.perform(delete(ID_ENDPOINT, 999L)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Review not found"));
    }

    @Test
    void createReview_WhenPropertyNotExists_ShouldReturnBadRequest() throws Exception {
        Mockito.when(reviewService.saveReview(any(Review.class), anyString()))
                .thenThrow(new ReviewException("Property not found"));

        mockMvc.perform(post(ROOT_ENDPOINT)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testReviewDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Property not found"));
    }

    @Test
    void createReview_WhenUserNotBooked_ShouldReturnBadRequest() throws Exception {
        Mockito.when(reviewService.saveReview(any(Review.class), anyString()))
                .thenThrow(new ReviewException("You can't leave a review until you've lived there"));

        mockMvc.perform(post(ROOT_ENDPOINT)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testReviewDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You can't leave a review until you've lived there"));
    }

    @Test
    void updateReview_WhenNotReviewOwner_ShouldReturnBadRequest() throws Exception {
        Mockito.when(reviewService.updateReview(any(Review.class), anyString()))
                .thenThrow(new ReviewException("You can only update your own reviews"));

        mockMvc.perform(put(ID_ENDPOINT, 1L)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testReviewDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You can only update your own reviews"));
    }
}