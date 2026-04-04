package com.example.propertyservice.controllers;

import com.example.propertyservice.BaseIntegrationTest;
import com.example.propertyservice.dto.PropertyDTO;
import com.example.propertyservice.models.Property;
import com.example.propertyservice.repositories.PropertyRepository;
import com.example.propertyservice.util.JwtTokenUtils;
import com.example.reviewservice.event.RatingUpdatedEvent;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.KafkaContainer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.awaitility.Awaitility.await;

class PropertyControllerIT extends BaseIntegrationTest {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private KafkaContainer kafkaContainer;

    @MockBean
    private JwtTokenUtils jwtTokenUtils;

    private KafkaTemplate<String, RatingUpdatedEvent> kafkaTemplate;

    private Property savedProperty;
    private final String validToken = "valid.token.here";

    @BeforeEach
    void setUp() {
        Mockito.when(jwtTokenUtils.getUserId(anyString())).thenReturn(1L);
        Mockito.when(jwtTokenUtils.getRoles(anyString())).thenReturn(List.of("ROLE_OWNER"));

        stubFor(get(urlEqualTo("/api/v1/users/1/exists"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Map<String, Object> producerProps = KafkaTestUtils.producerProps(kafkaContainer.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        producerProps.put("schema.registry.url", "mock://test-registry");

        ProducerFactory<String, RatingUpdatedEvent> pf = new DefaultKafkaProducerFactory<>(producerProps);
        kafkaTemplate = new KafkaTemplate<>(pf);

        Property property = new Property();
        property.setOwnerId(1L);
        property.setTitle("Existing Property in Database");
        property.setDescription("A very nice and cozy place for integration testing.");
        property.setLocation("Sydney");
        property.setPricePerNight(BigDecimal.valueOf(150.00));
        property.setCapacity(2);
        property.setAverageRating(BigDecimal.ZERO);
        property.setCreatedAt(LocalDateTime.now());
        property.setFeatures(new HashSet<>());

        savedProperty = propertyRepository.save(property);
    }

    @AfterEach
    void tearDown() {
        propertyRepository.deleteAll();
    }

    @Test
    void createProperty_WithValidData_ShouldSaveToDbAndReturnDto() throws Exception {
        PropertyDTO newProperty = new PropertyDTO();
        newProperty.setTitle("Brand New Property");
        newProperty.setDescription("Brand new place description");
        newProperty.setLocation("California");
        newProperty.setPricePerNight(BigDecimal.valueOf(100.00));
        newProperty.setCapacity(4);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/properties")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProperty)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Brand New Property"));

        assertThat(propertyRepository.findAll()).hasSize(2);
    }

    @Test
    void getPropertyById_ShouldReturnPropertyFromDb() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/properties/{id}", savedProperty.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedProperty.getId()))
                .andExpect(jsonPath("$.title").value("Existing Property in Database"));
    }

    @Test
    void updateProperty_ShouldUpdateInDbAndReturnDto() throws Exception {
        PropertyDTO updateDto = new PropertyDTO();
        updateDto.setTitle("Updated Title");
        updateDto.setDescription("Updated description");
        updateDto.setLocation("Sydney");
        updateDto.setPricePerNight(BigDecimal.valueOf(250.00));
        updateDto.setCapacity(3);

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/properties/{id}", savedProperty.getId())
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));

        Property updatedInDb = propertyRepository.findById(savedProperty.getId()).orElseThrow();
        assertThat(updatedInDb.getTitle()).isEqualTo("Updated Title");
        assertThat(updatedInDb.getPricePerNight()).isEqualByComparingTo(BigDecimal.valueOf(250.00));
    }

    @Test
    void deleteProperty_ShouldRemoveFromDb() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/properties/{id}", savedProperty.getId())
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());

        assertThat(propertyRepository.findById(savedProperty.getId())).isEmpty();
    }

    @Test
    void searchProperties_ShouldReturnFilteredProperties() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/properties/search")
                        .param("location", "Sydney")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(savedProperty.getId()));
    }

    @Test
    void getMyProperties_ShouldReturnPropertiesOfCurrentUser() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/properties/my")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void handleException_ShouldCatchValidationErrors() throws Exception {
        PropertyDTO invalidDto = new PropertyDTO();
        invalidDto.setTitle("");
        invalidDto.setPricePerNight(BigDecimal.valueOf(-10));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/properties")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldConsumeRatingUpdatedEvent_AndUpdatePropertyRatingInDatabase() throws Exception {
        RatingUpdatedEvent event = RatingUpdatedEvent.newBuilder()
                .setPropertyId(savedProperty.getId())
                .setNewAverageRating(4.8)
                .setTotalReviews(15L)
                .build();

        kafkaTemplate.send("rating-updated", String.valueOf(savedProperty.getId()), event).get();

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Property updatedProperty = propertyRepository.findById(savedProperty.getId()).orElseThrow();
            assertThat(updatedProperty.getAverageRating()).isEqualByComparingTo(BigDecimal.valueOf(4.8));
        });
    }
}