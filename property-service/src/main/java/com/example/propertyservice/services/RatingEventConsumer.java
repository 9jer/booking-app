package com.example.propertyservice.services;

import com.example.reviewservice.event.RatingUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingEventConsumer {

    private final PropertyService propertyService;

    @KafkaListener(topics = "rating-updated")
    public void listen(RatingUpdatedEvent ratingUpdatedEvent) {

        try {
            log.info("Start - Received from rating-updated topic: {}", ratingUpdatedEvent);
            propertyService.updateAverageRating(ratingUpdatedEvent.getPropertyId(), ratingUpdatedEvent.getNewAverageRating(), ratingUpdatedEvent.getTotalReviews());
            log.info("End - Rating updated successfully.");
        } catch (Exception e) {
            log.error("Error processing rating update for propertyId={}: {}",
                    ratingUpdatedEvent != null ? ratingUpdatedEvent.getPropertyId() : "null",
                    e.getMessage(), e);
        }

    }
}
