package com.example.reviewservice.event;

import com.example.reviewservice.repositories.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingEventProducer {

    private final KafkaTemplate<String, RatingUpdatedEvent> kafkaTemplate;
    private final ReviewRepository reviewRepository;

    public void sendRatingUpdatedEvent(Long propertyId) {
        Double averageRating = reviewRepository.getAverageRating(propertyId);
        Long totalReviews = reviewRepository.countReviews(propertyId);

        BigDecimal roundedRating = BigDecimal.valueOf(averageRating)
                .setScale(2, RoundingMode.HALF_UP);

        RatingUpdatedEvent event = RatingUpdatedEvent.newBuilder()
                .setPropertyId(propertyId)
                .setNewAverageRating(roundedRating.doubleValue())
                .setTotalReviews(totalReviews)
                .build();

        log.info("Start - Sending RatingUpdatedEvent {} to Kafka topic rating-updated", event);
        kafkaTemplate.send("rating-updated", event);
        log.info("End - Sending RatingUpdatedEvent {} to Kafka topic rating-updated", event);
    }
}
