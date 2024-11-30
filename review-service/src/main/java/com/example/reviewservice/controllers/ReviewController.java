package com.example.reviewservice.controllers;

import com.example.reviewservice.dto.ReviewDTO;
import com.example.reviewservice.dto.ReviewsResponse;
import com.example.reviewservice.models.Review;
import com.example.reviewservice.services.ReviewService;
import com.example.reviewservice.util.ErrorsUtil;
import com.example.reviewservice.util.ReviewErrorResponse;
import com.example.reviewservice.util.ReviewException;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {
    private final ReviewService reviewService;
    private final ModelMapper modelMapper;

    public ReviewController(ReviewService reviewService, ModelMapper modelMapper) {
        this.reviewService = reviewService;
        this.modelMapper = modelMapper;
    }

    @GetMapping("/property/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ReviewsResponse getReviewsByPropertyId(@PathVariable Long id) {
        return new ReviewsResponse(reviewService.getReviewsByPropertyId(id).stream()
                .map(this::convertReviewToReviewDTO).collect(Collectors.toList()));
    }

    @PostMapping
    public ResponseEntity<Review> createReview(@RequestHeader("Authorization") String authorizationHeader,
                                               @RequestBody @Valid ReviewDTO reviewDTO, BindingResult bindingResult) {
        Review review = convertReviewDTOToReview(reviewDTO);

        if(bindingResult.hasErrors()) {
            ErrorsUtil.returnAllErrors(bindingResult);
        }

        String jwtToken = authorizationHeader.replace("Bearer ", "");
        return ResponseEntity.ok(reviewService.saveReview(review, jwtToken));
    }

    private Review convertReviewDTOToReview(ReviewDTO reviewDTO) {
        Review review = new Review();
        review.setUserId(reviewDTO.getUserId());
        review.setPropertyId(reviewDTO.getPropertyId());
        review.setRating(reviewDTO.getRating());
        review.setComment(reviewDTO.getComment());

        return review;
    }

    private ReviewDTO convertReviewToReviewDTO(Review review) {
        return modelMapper.map(review, ReviewDTO.class);
    }

    @ExceptionHandler
    private ResponseEntity<Object> handleException(ReviewException e) {
        ReviewErrorResponse reviewErrorResponse = new ReviewErrorResponse(
                e.getMessage(),
                System.currentTimeMillis()
        );

        return new ResponseEntity<>(reviewErrorResponse, HttpStatus.BAD_REQUEST);
    }
}
