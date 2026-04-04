package com.example.bookingservice.controllers;

import com.example.bookingservice.BaseIntegrationTest;
import com.example.bookingservice.dto.BookingDTO;
import com.example.bookingservice.event.BookingCreatedEvent;
import com.example.bookingservice.models.Booking;
import com.example.bookingservice.models.BookingStatus;
import com.example.bookingservice.repositories.BookingRepository;
import com.example.bookingservice.util.JwtTokenUtils;
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
import org.testcontainers.containers.KafkaContainer;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingControllerIT extends BaseIntegrationTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private KafkaContainer kafkaContainer;

    @MockBean
    private JwtTokenUtils jwtTokenUtils;

    @BeforeEach
    void setUp() {
        Mockito.when(jwtTokenUtils.getUserId(anyString())).thenReturn(1L);
        Mockito.when(jwtTokenUtils.getEmail(anyString())).thenReturn("test@example.com");
        Mockito.when(jwtTokenUtils.getRoles(anyString())).thenReturn(List.of("ROLE_USER"));
    }

    @AfterEach
    void tearDown() {
        bookingRepository.deleteAll();
    }

    @Test
    void createBooking_ShouldSaveToDatabaseAndReturnCreated() throws Exception {
        BookingDTO requestDto = new BookingDTO();
        requestDto.setPropertyId(100L);
        requestDto.setCheckInDate(LocalDate.now().plusDays(1));
        requestDto.setCheckOutDate(LocalDate.now().plusDays(5));

        stubFor(get(urlEqualTo("/api/v1/properties/100/exists"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("true")));

        stubFor(get(urlEqualTo("/api/v1/properties/100"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                            {
                                "id": 100,
                                "pricePerNight": 100.0,
                                "ownerId": 2,
                                "title": "Test Apartment"
                            }
                            """)));

        stubFor(get(urlEqualTo("/api/v1/users/1/exists"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("true")));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/bookings")
                        .header("Authorization", "Bearer dummy.token.here")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.propertyId").value(100L))
                .andExpect(jsonPath("$.status").value("PENDING"));

        List<Booking> savedBookings = bookingRepository.findAll();
        assertThat(savedBookings).hasSize(1);
        assertThat(savedBookings.get(0).getPropertyId()).isEqualTo(100L);
    }

    @Test
    void getBookingById_ShouldReturnFromDatabase() throws Exception {
        Booking booking = new Booking();
        booking.setUserId(1L);
        booking.setPropertyId(200L);
        booking.setCheckInDate(LocalDate.now().plusDays(10));
        booking.setCheckOutDate(LocalDate.now().plusDays(15));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCreatedAt(LocalDateTime.now());
        Booking savedBooking = bookingRepository.save(booking);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/bookings/{id}", savedBooking.getId())
                        .header("Authorization", "Bearer dummy.token.here")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedBooking.getId()))
                .andExpect(jsonPath("$.propertyId").value(200L))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void updateBookingStatusToConfirmed_ShouldSendKafkaEvent() throws Exception {
        Booking booking = new Booking();
        booking.setUserId(2L);
        booking.setPropertyId(150L);
        booking.setCheckInDate(LocalDate.now().plusDays(10));
        booking.setCheckOutDate(LocalDate.now().plusDays(15));
        booking.setStatus(BookingStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());
        Booking savedBooking = bookingRepository.save(booking);

        stubFor(get(urlEqualTo("/api/v1/properties/150"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                                {
                                    "id": 150,
                                    "pricePerNight": 100.0,
                                    "ownerId": 1, 
                                    "title": "Luxury Villa"
                                }
                                """)));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/bookings/" + savedBooking.getId() + "/status")
                        .header("Authorization", "Bearer dummy.token.here")
                        .param("status", "CONFIRMED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                kafkaContainer.getBootstrapServers(),
                "test-group",
                "true"
        );

        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put("schema.registry.url", "mock://test-registry");
        consumerProps.put("specific.avro.reader", "true");

        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);

        ConsumerFactory<String, BookingCreatedEvent> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<String, BookingCreatedEvent> consumer = cf.createConsumer();

        consumer.subscribe(Collections.singletonList("booking-created"));

        ConsumerRecord<String, BookingCreatedEvent> record = KafkaTestUtils.getSingleRecord(consumer, "booking-created", Duration.ofSeconds(5));

        assertThat(record).isNotNull();
        BookingCreatedEvent event = record.value();
        assertThat(event.getBookingId()).isEqualTo(savedBooking.getId());
        assertThat(event.getPropertyName().toString()).isEqualTo("Luxury Villa");
        assertThat(event.getEmail().toString()).isEqualTo("test@example.com");

        consumer.close();
    }

    @Test
    void getBookingsByPropertyId_ShouldReturnPagedBookings() throws Exception {
        Booking booking = new Booking();
        booking.setUserId(2L);
        booking.setPropertyId(180L);
        booking.setCheckInDate(LocalDate.now().plusDays(1));
        booking.setCheckOutDate(LocalDate.now().plusDays(5));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCreatedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        stubFor(get(urlEqualTo("/api/v1/properties/180"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"id\": 180, \"pricePerNight\": 100.0, \"ownerId\": 1, \"title\": \"House\"}")));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/bookings/property/180")
                        .header("Authorization", "Bearer dummy.token.here")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(booking.getId()));
    }

    @Test
    void getRecentBookings_ShouldReturnUserBookings() throws Exception {
        Booking booking = new Booking();
        booking.setUserId(1L);
        booking.setPropertyId(190L);
        booking.setCheckInDate(LocalDate.now().minusDays(5));
        booking.setCheckOutDate(LocalDate.now().minusDays(1));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCreatedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        stubFor(get(urlEqualTo("/api/v1/properties/190"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"id\": 190, \"pricePerNight\": 100.0, \"ownerId\": 2, \"title\": \"House\"}")));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/bookings/recent")
                        .header("Authorization", "Bearer dummy.token.here")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(booking.getId()));
    }

    @Test
    void initPayment_ShouldReturnPaymentUrl() throws Exception {
        Booking booking = new Booking();
        booking.setUserId(1L);
        booking.setPropertyId(200L);
        booking.setCheckInDate(LocalDate.now().plusDays(10));
        booking.setCheckOutDate(LocalDate.now().plusDays(15));
        booking.setStatus(BookingStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());
        Booking savedBooking = bookingRepository.save(booking);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/bookings/" + savedBooking.getId() + "/payment")
                        .header("Authorization", "Bearer dummy.token.here")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentUrl").exists());
    }

    @Test
    void handlePaymentSuccess_ShouldCompletePayment() throws Exception {
        Booking booking = new Booking();
        booking.setUserId(1L);
        booking.setPropertyId(210L);
        booking.setCheckInDate(LocalDate.now().plusDays(10));
        booking.setCheckOutDate(LocalDate.now().plusDays(15));
        booking.setStatus(BookingStatus.AWAITING_PAYMENT);
        booking.setCreatedAt(LocalDateTime.now());
        Booking savedBooking = bookingRepository.save(booking);

        stubFor(get(urlEqualTo("/api/v1/properties/210"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"id\": 210, \"pricePerNight\": 100.0, \"ownerId\": 2, \"title\": \"House\"}")));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/bookings/" + savedBooking.getId() + "/payment/success")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Booking updatedBooking = bookingRepository.findById(savedBooking.getId()).orElseThrow();
        assertThat(updatedBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void cancelBooking_ShouldSetStatusToCancelled() throws Exception {
        Booking booking = new Booking();
        booking.setUserId(1L);
        booking.setPropertyId(220L);
        booking.setCheckInDate(LocalDate.now().plusDays(10));
        booking.setCheckOutDate(LocalDate.now().plusDays(15));
        booking.setStatus(BookingStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());
        Booking savedBooking = bookingRepository.save(booking);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/bookings/" + savedBooking.getId())
                        .header("Authorization", "Bearer dummy.token.here")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        Booking updatedBooking = bookingRepository.findById(savedBooking.getId()).orElseThrow();
        assertThat(updatedBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void createBooking_WithCheckOutBeforeCheckIn_ShouldReturnBadRequest() throws Exception {
        BookingDTO requestDto = new BookingDTO();
        requestDto.setPropertyId(100L);
        requestDto.setCheckInDate(LocalDate.now().plusDays(5));
        requestDto.setCheckOutDate(LocalDate.now().plusDays(1));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/bookings")
                        .header("Authorization", "Bearer dummy.token.here")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());

        List<Booking> savedBookings = bookingRepository.findAll();
        assertThat(savedBookings).isEmpty();
    }

    @Test
    void updateBookingStatus_WhenUserIsNotOwner_ShouldReturnForbidden() throws Exception {
        Booking booking = new Booking();
        booking.setUserId(2L);
        booking.setPropertyId(150L);
        booking.setCheckInDate(LocalDate.now().plusDays(10));
        booking.setCheckOutDate(LocalDate.now().plusDays(15));
        booking.setStatus(BookingStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());
        Booking savedBooking = bookingRepository.save(booking);

        stubFor(get(urlEqualTo("/api/v1/properties/150"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                                {
                                    "id": 150,
                                    "pricePerNight": 100.0,
                                    "ownerId": 99, 
                                    "title": "Luxury Villa"
                                }
                                """)));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/bookings/" + savedBooking.getId() + "/status")
                        .header("Authorization", "Bearer dummy.token.here")
                        .param("status", "CONFIRMED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());

        Booking bookingInDb = bookingRepository.findById(savedBooking.getId()).orElseThrow();
        assertThat(bookingInDb.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void createBooking_WhenPropertyServiceIsDown_ShouldTriggerFallback() throws Exception {
        BookingDTO requestDto = new BookingDTO();
        requestDto.setPropertyId(100L);
        requestDto.setCheckInDate(LocalDate.now().plusDays(1));
        requestDto.setCheckOutDate(LocalDate.now().plusDays(5));

        stubFor(get(urlEqualTo("/api/v1/properties/100/exists"))
                .willReturn(aResponse()
                        .withStatus(503)));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/bookings")
                        .header("Authorization", "Bearer dummy.token.here")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().is4xxClientError());

        List<Booking> savedBookings = bookingRepository.findAll();
        assertThat(savedBookings).isEmpty();
    }

    @Test
    void createBooking_WhenUserServiceIsDown_ShouldTriggerFallback() throws Exception {
        BookingDTO requestDto = new BookingDTO();
        requestDto.setPropertyId(100L);
        requestDto.setCheckInDate(LocalDate.now().plusDays(1));
        requestDto.setCheckOutDate(LocalDate.now().plusDays(5));

        stubFor(get(urlEqualTo("/api/v1/properties/100/exists"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("true")));

        stubFor(get(urlEqualTo("/api/v1/users/1/exists"))
                .willReturn(aResponse()
                        .withStatus(500)));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/bookings")
                        .header("Authorization", "Bearer dummy.token.here")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().is4xxClientError());

        List<Booking> savedBookings = bookingRepository.findAll();
        assertThat(savedBookings).isEmpty();
    }
}