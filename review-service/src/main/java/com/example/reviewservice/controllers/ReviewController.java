package com.example.reviewservice.controllers;

import com.example.reviewservice.dto.GetReviewDTO;
import com.example.reviewservice.dto.ReviewDTO;
import com.example.reviewservice.models.Review;
import com.example.reviewservice.services.ReviewService;
import com.example.reviewservice.util.ErrorsUtil;
import com.example.reviewservice.util.ReviewErrorResponse;
import com.example.reviewservice.util.ReviewException;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${application.endpoint.root}")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;
    private final ModelMapper modelMapper;

    @GetMapping(path = "${application.endpoint.reviews-by-property-id}")
    public ResponseEntity<Page<GetReviewDTO>> getReviewsByPropertyId(@PathVariable Long id,
                                                                     @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(reviewService.getReviewsByPropertyId(id, pageable));
    }

    @PostMapping
    public ResponseEntity<GetReviewDTO> createReview(@Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader,
                                                     @RequestBody @Valid ReviewDTO reviewDTO, BindingResult bindingResult) {

        if(bindingResult.hasErrors()) {
            ErrorsUtil.returnAllErrors(bindingResult);
        }

        Review review = convertReviewDTOToReview(reviewDTO);
        String jwtToken = authorizationHeader.replace("Bearer ", "");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(reviewService.saveReview(review, jwtToken));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GetReviewDTO> updateReview(@Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader
            , @PathVariable(name = "id") Long id, @RequestBody @Valid ReviewDTO reviewDTO, BindingResult bindingResult) {

        if(bindingResult.hasErrors()) {
            ErrorsUtil.returnAllErrors(bindingResult);
        }

        String jwtToken = authorizationHeader.replace("Bearer ", "");
        Review review = convertReviewDTOToReview(reviewDTO);
        review.setId(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(reviewService.updateReview(review, jwtToken));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader
            , @PathVariable(name = "id") Long id) {
        String jwtToken = authorizationHeader.replace("Bearer ", "");
        reviewService.deleteReview(id, jwtToken);

        return ResponseEntity.noContent().build();
    }

    private Review convertReviewDTOToReview(ReviewDTO reviewDTO) {
        return modelMapper.map(reviewDTO, Review.class);
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