package com.example.reviewservice.controllers;

import com.example.reviewservice.dto.GetReviewDTO;
import com.example.reviewservice.dto.ReviewDTO;
import com.example.reviewservice.dto.ReviewsResponse;
import com.example.reviewservice.models.Review;
import com.example.reviewservice.services.ReviewService;
import com.example.reviewservice.util.ErrorsUtil;
import com.example.reviewservice.util.ReviewErrorResponse;
import com.example.reviewservice.util.ReviewException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("${application.endpoint.root}")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;
    private final ModelMapper modelMapper;

    @GetMapping(path = "${application.endpoint.reviews-by-property-id}")
    public ResponseEntity<ReviewsResponse> getReviewsByPropertyId(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ReviewsResponse(reviewService.getReviewsByPropertyId(id).stream()
                        .map(this::convertReviewToGetReviewDTO).collect(Collectors.toList())));
    }

    @PostMapping
    public ResponseEntity<Review> createReview(@RequestHeader("Authorization") String authorizationHeader,
            @RequestBody @Valid ReviewDTO reviewDTO, BindingResult bindingResult) {
        Review review = convertReviewDTOToReview(reviewDTO);

        if(bindingResult.hasErrors()) {
            ErrorsUtil.returnAllErrors(bindingResult);
        }

        String jwtToken = authorizationHeader.replace("Bearer ", "");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(reviewService.saveReview(review, jwtToken));
    }

    private Review convertReviewDTOToReview(ReviewDTO reviewDTO) {
      /*  Review review = new Review();
        review.setPropertyId(reviewDTO.getPropertyId());
        review.setRating(reviewDTO.getRating());
        review.setComment(reviewDTO.getComment());*/

        return modelMapper.map(reviewDTO, Review.class);
    }

    private GetReviewDTO convertReviewToGetReviewDTO(Review review) {
        return modelMapper.map(review, GetReviewDTO.class);
    }

    @ExceptionHandler
    private ResponseEntity<Object> handleException(ReviewException e) {
        ReviewErrorResponse reviewErrorResponse = new ReviewErrorResponse(
                e.getMessage(),
                System.currentTimeMillis()
        );

        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(reviewErrorResponse);
    }
}
