package com.example.propertyservice.services;

import com.example.reviewservice.event.RatingUpdatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingEventConsumerTest {

    @Mock
    private PropertyService propertyService;

    @InjectMocks
    private RatingEventConsumer ratingEventConsumer;

    @Test
    void listen_ValidEvent_UpdatesRating() {
        // Given
        RatingUpdatedEvent event = new RatingUpdatedEvent();
        event.setPropertyId(1L);
        event.setNewAverageRating(4.5);
        event.setTotalReviews(10L);

        // When
        ratingEventConsumer.listen(event);

        // Then
        verify(propertyService, times(1)).updateAverageRating(1L, 4.5, 10L);
    }

    @Test
    void listen_ExceptionInService_LogsErrorAndDoesNotThrow() {
        // Given
        RatingUpdatedEvent event = new RatingUpdatedEvent();
        event.setPropertyId(1L);

        doThrow(new RuntimeException("Database error"))
                .when(propertyService).updateAverageRating(any(), any(), any());

        // When & Then
        assertDoesNotThrow(() -> ratingEventConsumer.listen(event));

        verify(propertyService, times(1)).updateAverageRating(any(), any(), any());
    }
}