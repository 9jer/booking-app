package com.example.reviewservice.event;

import com.example.reviewservice.models.Review;
import com.example.reviewservice.repositories.ReviewRepository;
import com.example.reviewservice.services.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingEventProducer {

    private final KafkaTemplate<String, RatingUpdatedEvent> kafkaTemplate;
    private final ReviewRepository reviewRepository;

    public void sendRatingUpdatedEvent(Long propertyId) {
        List<Review> reviews = reviewRepository.findByPropertyId(propertyId);
        System.out.println(reviews);
        double averageRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        BigDecimal roundedRating = BigDecimal.valueOf(averageRating)
                .setScale(2, RoundingMode.HALF_UP);

        var event = new RatingUpdatedEvent();
        event.setPropertyId(propertyId);
        event.setNewAverageRating(roundedRating.doubleValue());
        event.setTotalReviews((long) reviews.size());

        log.info("Start - Sending RatingUpdatedEvent {} to Kafka topic rating-updated", event);
        kafkaTemplate.send("rating-updated", event);
        log.info("End - Sending RatingUpdatedEvent {} to Kafka topic rating-updated", event);
    }
}
