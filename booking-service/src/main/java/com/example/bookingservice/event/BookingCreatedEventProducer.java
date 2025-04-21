package com.example.bookingservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingCreatedEventProducer {

    private final KafkaTemplate<String, BookingCreatedEvent> kafkaTemplate;

    public void sendBookingCreatedEvent(String topic, BookingCreatedEvent bookingCreatedEvent) {
        log.info("Start - Sending BookingCreatedEvent with booking create {} to Kafka topic booking-created", bookingCreatedEvent);
        kafkaTemplate.send(topic, bookingCreatedEvent);
        log.info("End - Sending BookingCreatedEvent with booking create {} to Kafka topic booking-created", bookingCreatedEvent);
    }
}
