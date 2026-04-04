package com.example.reviewservice.controllers;

import com.example.reviewservice.BaseIntegrationTest;
import com.example.reviewservice.dto.ReviewDTO;
import com.example.reviewservice.event.RatingUpdatedEvent;
import com.example.reviewservice.models.Review;
import com.example.reviewservice.repositories.ReviewRepository;
import com.example.reviewservice.util.JwtTokenUtils;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.KafkaContainer;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ReviewControllerIT extends BaseIntegrationTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private KafkaContainer kafkaContainer;

    @MockBean
    private JwtTokenUtils jwtTokenUtils;

    private final String validToken = "dummy.jwt.token";

    @BeforeEach
    void setUp() {
        Mockito.when(jwtTokenUtils.getUserId(anyString())).thenReturn(1L);
        Mockito.when(jwtTokenUtils.getRoles(anyString())).thenReturn(List.of("ROLE_USER"));
    }

    @AfterEach
    void tearDown() {
        reviewRepository.deleteAll();
    }

    @Test
    void createReview_ShouldSaveToDb_AndSendKafkaEvent() throws Exception {
        ReviewDTO requestDto = new ReviewDTO();
        requestDto.setPropertyId(100L);
        requestDto.setRating(5);
        requestDto.setComment("Amazing place, highly recommended!");

        stubFor(get(urlEqualTo("/api/v1/properties/100/exists"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("true")));

        stubFor(get(urlEqualTo("/api/v1/users/1/exists"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("true")));

        stubFor(get(urlEqualTo("/api/v1/bookings/was-booked?propertyId=100&userId=1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("true")));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.rating").value(5));

        List<Review> savedReviews = reviewRepository.findAll();
        assertThat(savedReviews).hasSize(1);
        assertThat(savedReviews.get(0).getComment()).isEqualTo("Amazing place, highly recommended!");

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                kafkaContainer.getBootstrapServers(),
                "review-test-group",
                "true"
        );
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put("schema.registry.url", "mock://test-registry");
        consumerProps.put("specific.avro.reader", "true");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);

        ConsumerFactory<String, RatingUpdatedEvent> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<String, RatingUpdatedEvent> consumer = cf.createConsumer();
        consumer.subscribe(Collections.singletonList("rating-updated"));

        ConsumerRecord<String, RatingUpdatedEvent> record = KafkaTestUtils.getSingleRecord(consumer, "rating-updated", Duration.ofSeconds(5));

        assertThat(record).isNotNull();
        RatingUpdatedEvent event = record.value();
        assertThat(event.getPropertyId()).isEqualTo(100L);
        assertThat(event.getNewAverageRating()).isEqualTo(5.0);
        assertThat(event.getTotalReviews()).isEqualTo(1L);

        consumer.close();
    }

    @Test
    void createReview_WhenUserDidNotBook_ShouldReturnForbidden() throws Exception {
        ReviewDTO requestDto = new ReviewDTO();
        requestDto.setPropertyId(100L);
        requestDto.setRating(5);
        requestDto.setComment("Fake review!");

        stubFor(get(urlEqualTo("/api/v1/properties/100/exists"))
                .willReturn(aResponse().withStatus(200).withBody("true")));

        stubFor(get(urlEqualTo("/api/v1/users/1/exists"))
                .willReturn(aResponse().withStatus(200).withBody("true")));

        stubFor(get(urlEqualTo("/api/v1/bookings/was-booked?propertyId=100&userId=1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("false")));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().is4xxClientError());

        assertThat(reviewRepository.findAll()).isEmpty();
    }

    @Test
    void getReviewsByPropertyId_ShouldReturnPagedReviews() throws Exception {
        Review review = new Review();
        review.setPropertyId(200L);
        review.setUserId(1L);
        review.setRating(4);
        review.setComment("Good stay");
        reviewRepository.save(review);

        stubFor(get(urlEqualTo("/api/v1/users/1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"id\": 1, \"firstName\": \"John\", \"lastName\": \"Doe\"}")));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/reviews/property/200")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].rating").value(4));
    }
}