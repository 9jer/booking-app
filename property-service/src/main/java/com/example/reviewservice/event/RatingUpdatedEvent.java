package com.example.reviewservice.event;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RatingUpdatedEvent {

    private Long propertyId;
    private Double newAverageRating;
    private Long totalReviews;
}