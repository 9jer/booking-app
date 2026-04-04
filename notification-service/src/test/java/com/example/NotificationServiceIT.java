package com.example;

import com.example.bookingservice.event.BookingCreatedEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.group-id=notification-test-group-${random.uuid}",
        "spring.kafka.properties.schema.registry.url=mock://test-registry",
        "spring.kafka.consumer.properties.schema.registry.url=mock://test-registry",
        "spring.kafka.consumer.properties.specific.avro.reader=true",
        "management.health.mail.enabled=false"
})
@Import(NotificationServiceIT.TestConfig.class)
@ActiveProfiles("test")
class NotificationServiceIT {

    @MockBean
    private JavaMailSender javaMailSender;

    @Autowired
    private KafkaTemplate<String, BookingCreatedEvent> kafkaTemplate;

    @Test
    void shouldConsumeKafkaMessageAndSendEmail() {
        BookingCreatedEvent event = new BookingCreatedEvent();
        event.setBookingId(100L);
        event.setEmail("user@example.com");
        event.setPropertyName("Ocean View Apartment");
        event.setCheckInDate("2026-05-01");
        event.setCheckOutDate("2026-05-10");

        kafkaTemplate.send("booking-created", String.valueOf(event.getBookingId()), event);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(javaMailSender).send(any(MimeMessagePreparator.class));
        });
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @ServiceConnection
        KafkaContainer kafkaContainer() {
            return new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));
        }

        @Bean
        public ProducerFactory<String, BookingCreatedEvent> producerFactory(KafkaContainer kafkaContainer) {
            Map<String, Object> configProps = new HashMap<>();
            configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
            configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
            configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, io.confluent.kafka.serializers.KafkaAvroSerializer.class);
            configProps.put("schema.registry.url", "mock://test-registry");
            return new DefaultKafkaProducerFactory<>(configProps);
        }

        @Bean
        public KafkaTemplate<String, BookingCreatedEvent> kafkaTemplate(ProducerFactory<String, BookingCreatedEvent> producerFactory) {
            return new KafkaTemplate<>(producerFactory);
        }
    }
}