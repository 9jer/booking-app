package com.example.bookingservice.controllers;

import com.example.bookingservice.dto.BookingDTO;
import com.example.bookingservice.dto.GetBookingDTO;
import com.example.bookingservice.models.Booking;
import com.example.bookingservice.models.BookingStatus;
import com.example.bookingservice.services.BookingService;
import com.example.bookingservice.util.JwtTokenUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@AutoConfigureMockMvc(addFilters = false)
class BookingControllerIT {

    private static final String ROOT_ENDPOINT = "/api/v1/bookings";
    private static final String ID_ENDPOINT = ROOT_ENDPOINT + "/{id}";
    private static final String HISTORY_ENDPOINT = ROOT_ENDPOINT + "/history/{id}";
    private static final String STATUS_ENDPOINT = ID_ENDPOINT + "/status";
    private static final String AVAILABILITY_ENDPOINT = ROOT_ENDPOINT + "/availability";
    private static final String WAS_BOOKED_ENDPOINT = ROOT_ENDPOINT + "/was-booked";
    private static final String AVAILABLE_DATES_ENDPOINT = ROOT_ENDPOINT + "/available-dates";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private JwtTokenUtils jwtTokenUtils;

    @MockBean
    private ModelMapper modelMapper;

    private Booking testBooking;
    private BookingDTO testBookingDTO;
    private GetBookingDTO testGetBookingDTO;
    private String validToken = "valid.token.here";

    @BeforeEach
    void setUp() {
        testBooking = new Booking();
        testBooking.setId(1L);
        testBooking.setUserId(1L);
        testBooking.setPropertyId(1L);
        testBooking.setCheckInDate(LocalDate.now().plusDays(1));
        testBooking.setCheckOutDate(LocalDate.now().plusDays(3));
        testBooking.setStatus(BookingStatus.PENDING);
        testBooking.setCreatedAt(LocalDateTime.now());

        testBookingDTO = new BookingDTO();
        testBookingDTO.setPropertyId(1L);
        testBookingDTO.setCheckInDate(LocalDate.now().plusDays(1));
        testBookingDTO.setCheckOutDate(LocalDate.now().plusDays(3));
        testBookingDTO.setStatus(BookingStatus.PENDING);

        testGetBookingDTO = new GetBookingDTO();
        testGetBookingDTO.setId(1L);
        testGetBookingDTO.setUserId(1L);
        testGetBookingDTO.setPropertyId(1L);
        testGetBookingDTO.setCheckInDate(LocalDate.now().plusDays(1));
        testGetBookingDTO.setCheckOutDate(LocalDate.now().plusDays(3));
        testGetBookingDTO.setStatus(BookingStatus.PENDING);

        Mockito.when(jwtTokenUtils.getUserId(anyString())).thenReturn(1L);
        Mockito.when(jwtTokenUtils.getEmail(anyString())).thenReturn("test@example.com");
        Mockito.when(jwtTokenUtils.getRoles(anyString())).thenReturn(List.of("ROLE_USER"));
    }

    @Test
    void getAllBookings_ShouldReturnListOfBookings() throws Exception {
        Mockito.when(bookingService.getAllBookings()).thenReturn(List.of(testBooking));
        Mockito.when(modelMapper.map(any(Booking.class), eq(GetBookingDTO.class))).thenReturn(testGetBookingDTO);

        mockMvc.perform(MockMvcRequestBuilders.get(ROOT_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookings[0].id").value(testGetBookingDTO.getId()))
                .andExpect(jsonPath("$.bookings[0].propertyId").value(testGetBookingDTO.getPropertyId()));
    }

    @Test
    void getBookingById_ShouldReturnBooking() throws Exception {
        Mockito.when(bookingService.getBookingById(anyLong())).thenReturn(testBooking);
        Mockito.when(modelMapper.map(any(Booking.class), eq(GetBookingDTO.class))).thenReturn(testGetBookingDTO);

        mockMvc.perform(MockMvcRequestBuilders.get(ID_ENDPOINT, 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testGetBookingDTO.getId()))
                .andExpect(jsonPath("$.propertyId").value(testGetBookingDTO.getPropertyId()));
    }

    @Test
    void getBookingHistoryById_ShouldReturnBookingHistory() throws Exception {
        Mockito.when(bookingService.getBookingHistoryByBookingId(anyLong()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(MockMvcRequestBuilders.get(HISTORY_ENDPOINT, 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history").isArray());
    }

    @Test
    void createBooking_WithValidData_ShouldReturnCreatedBooking() throws Exception {
        Mockito.when(bookingService.createBooking(any(Booking.class), anyString()))
                .thenReturn(testBooking);
        Mockito.when(modelMapper.map(any(BookingDTO.class), eq(Booking.class))).thenReturn(testBooking);
        Mockito.when(modelMapper.map(any(Booking.class), eq(GetBookingDTO.class))).thenReturn(testGetBookingDTO);

        mockMvc.perform(MockMvcRequestBuilders.post(ROOT_ENDPOINT)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBookingDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(testGetBookingDTO.getId()));
    }

    @Test
    void updateBookingStatus_ShouldReturnUpdatedBooking() throws Exception {
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testGetBookingDTO.setStatus(BookingStatus.CONFIRMED);

        Mockito.when(bookingService.updateBookingStatus(anyLong(), any(BookingStatus.class)))
                .thenReturn(testBooking);
        Mockito.when(modelMapper.map(any(Booking.class), eq(GetBookingDTO.class))).thenReturn(testGetBookingDTO);

        mockMvc.perform(MockMvcRequestBuilders.patch(STATUS_ENDPOINT, 1L)
                        .param("status", "CONFIRMED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void isAvailable_ShouldReturnBoolean() throws Exception {
        Mockito.when(bookingService.isAvailable(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.get(AVAILABILITY_ENDPOINT)
                        .param("propertyId", "1")
                        .param("checkIn", LocalDate.now().plusDays(1).toString())
                        .param("checkOut", LocalDate.now().plusDays(3).toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void wasBooked_ShouldReturnBoolean() throws Exception {
        Mockito.when(bookingService.whetherThereWasABooking(anyLong(), anyLong()))
                .thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.get(WAS_BOOKED_ENDPOINT)
                        .param("propertyId", "1")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void getAvailableDates_ShouldReturnListOfDates() throws Exception {
        List<LocalDate> availableDates = List.of(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
        Mockito.when(bookingService.getAvailableDates(anyLong()))
                .thenReturn(availableDates);

        mockMvc.perform(MockMvcRequestBuilders.get(AVAILABLE_DATES_ENDPOINT)
                        .param("propertyId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableDates[0]").value(availableDates.get(0).toString()))
                .andExpect(jsonPath("$.availableDates[1]").value(availableDates.get(1).toString()));
    }
}