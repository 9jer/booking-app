package com.example.bookingservice.controllers;

import com.example.bookingservice.dto.*;
import com.example.bookingservice.models.Booking;
import com.example.bookingservice.models.BookingHistory;
import com.example.bookingservice.models.BookingStatus;
import com.example.bookingservice.services.BookingService;
import com.example.bookingservice.util.BookingException;
import com.example.bookingservice.util.ErrorsUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import java.net.URI;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private BookingService bookingService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private BookingController bookingController;

    private Booking booking;
    private BookingDTO bookingDTO;
    private GetBookingDTO getBookingDTO;
    private BookingHistory bookingHistory;
    private BookingHistoryDTO bookingHistoryDTO;

    @BeforeEach
    void setUp() {
        booking = new Booking();
        booking.setId(1L);
        booking.setUserId(1L);
        booking.setPropertyId(1L);
        booking.setCheckInDate(LocalDate.now().plusDays(1));
        booking.setCheckOutDate(LocalDate.now().plusDays(3));
        booking.setStatus(BookingStatus.CONFIRMED);

        bookingDTO = new BookingDTO();
        bookingDTO.setPropertyId(1L);
        bookingDTO.setCheckInDate(LocalDate.now().plusDays(1));
        bookingDTO.setCheckOutDate(LocalDate.now().plusDays(3));
        bookingDTO.setStatus(BookingStatus.CONFIRMED);

        getBookingDTO = new GetBookingDTO();
        getBookingDTO.setId(1L);
        getBookingDTO.setUserId(1L);
        getBookingDTO.setPropertyId(1L);
        getBookingDTO.setCheckInDate(LocalDate.now().plusDays(1));
        getBookingDTO.setCheckOutDate(LocalDate.now().plusDays(3));
        getBookingDTO.setStatus(BookingStatus.CONFIRMED);

        bookingHistory = new BookingHistory();
        bookingHistory.setId(1L);
        bookingHistory.setId(1L);
        bookingHistory.setStatus(String.valueOf(BookingStatus.CONFIRMED));

        bookingHistoryDTO = new BookingHistoryDTO();
        bookingHistoryDTO.setBooking(booking);
        bookingHistoryDTO.setStatus(String.valueOf(BookingStatus.CONFIRMED));
    }

    @Test
    void getAllBookings_ReturnsValidResponseEntity() {
        // Given
        when(bookingService.getAllBookings()).thenReturn(Collections.singletonList(booking));
        when(modelMapper.map(any(Booking.class), eq(GetBookingDTO.class))).thenReturn(getBookingDTO);

        // When
        ResponseEntity<BookingsResponse> response = bookingController.getAllBookings();

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getBookings().size());
        assertEquals(getBookingDTO, response.getBody().getBookings().get(0));

        verify(bookingService, times(1)).getAllBookings();
        verify(modelMapper, times(1)).map(any(Booking.class), eq(GetBookingDTO.class));
    }

    @Test
    void getBookingById_BookingExists_ReturnsValidResponseEntity() {
        // Given
        when(bookingService.getBookingById(1L)).thenReturn(booking);
        when(modelMapper.map(any(Booking.class), eq(GetBookingDTO.class))).thenReturn(getBookingDTO);

        // When
        ResponseEntity<GetBookingDTO> response = bookingController.getBookingById(1L);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(getBookingDTO, response.getBody());

        verify(bookingService, times(1)).getBookingById(1L);
        verify(modelMapper, times(1)).map(any(Booking.class), eq(GetBookingDTO.class));
    }

    @Test
    void getBookingById_BookingNotExists_ThrowsBookingException() {
        // Given
        when(bookingService.getBookingById(1L)).thenThrow(new BookingException("Booking not found"));

        // When & Then
        assertThrows(BookingException.class, () -> bookingController.getBookingById(1L));
        verify(bookingService, times(1)).getBookingById(1L);
        verify(modelMapper, never()).map(any(Booking.class), eq(GetBookingDTO.class));
    }

    @Test
    void getBookingHistoryById_HistoryExists_ReturnsValidResponseEntity() {
        // Given
        when(bookingService.getBookingHistoryByBookingId(1L)).thenReturn(Collections.singletonList(bookingHistory));
        when(modelMapper.map(any(BookingHistory.class), eq(BookingHistoryDTO.class))).thenReturn(bookingHistoryDTO);

        // When
        ResponseEntity<BookingHistoryResponse> response = bookingController.getBookingHistoryById(1L);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());

        assertEquals(1, response.getBody().getHistory().size());
        assertEquals(bookingHistoryDTO, response.getBody().getHistory().get(0));


        verify(bookingService, times(1)).getBookingHistoryByBookingId(1L);
        verify(modelMapper, times(1)).map(any(BookingHistory.class), eq(BookingHistoryDTO.class));
    }

    @Test
    void createBooking_ValidRequest_ReturnsCreatedResponse() {
        // Given
        String authHeader = "Bearer token";
        when(bindingResult.hasErrors()).thenReturn(false);
        when(modelMapper.map(any(BookingDTO.class), eq(Booking.class))).thenReturn(booking);
        when(bookingService.createBooking(any(Booking.class), anyString())).thenReturn(booking);

        // When
        ResponseEntity<Booking> response = bookingController.createBooking(authHeader, bookingDTO, bindingResult);

        // Then
        assertEquals(201, response.getStatusCodeValue());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertNotNull(response.getHeaders().getLocation());
        assertTrue(Objects.requireNonNull(response.getHeaders().getLocation()).toString().contains("/1"));

        verify(bookingService, times(1)).createBooking(any(Booking.class), anyString());
        verify(modelMapper, times(1)).map(any(BookingDTO.class), eq(Booking.class));
    }

    @Test
    void createBooking_InvalidRequest_ThrowsBookingException() {
        // Given
        String authHeader = "Bearer token";
        when(bindingResult.hasErrors()).thenReturn(true);

        // When & Then
        assertThrows(BookingException.class,
                () -> bookingController.createBooking(authHeader, bookingDTO, bindingResult));

        verify(bindingResult, times(1)).hasErrors();
        verify(bookingService, never()).createBooking(any(Booking.class), anyString());
    }

    @Test
    void updateBookingStatus_ValidRequest_ReturnsUpdatedBooking() {
        // Given
        when(bookingService.updateBookingStatus(1L, BookingStatus.CANCELLED)).thenReturn(booking);

        // When
        ResponseEntity<Booking> response = bookingController.updateBookingStatus(1L, BookingStatus.CANCELLED);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(booking, response.getBody());

        verify(bookingService, times(1)).updateBookingStatus(1L, BookingStatus.CANCELLED);
    }

    @Test
    void isAvailable_PropertyAvailable_ReturnsTrue() {
        // Given
        Long propertyId = 1L;
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);
        when(bookingService.isAvailable(propertyId, checkIn, checkOut)).thenReturn(true);

        // When
        ResponseEntity<Boolean> response = bookingController.isAvailable(propertyId, checkIn, checkOut);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody());

        verify(bookingService, times(1)).isAvailable(propertyId, checkIn, checkOut);
    }

    @Test
    void wasBooked_PropertyWasBooked_ReturnsTrue() {
        // Given
        Long propertyId = 1L;
        Long userId = 1L;
        when(bookingService.whetherThereWasABooking(propertyId, userId)).thenReturn(true);

        // When
        ResponseEntity<Boolean> response = bookingController.wasBooked(propertyId, userId);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody());

        verify(bookingService, times(1)).whetherThereWasABooking(propertyId, userId);
    }

    @Test
    void getAvailableDates_PropertyHasAvailableDates_ReturnsDatesList() {
        // Given
        Long propertyId = 1L;
        List<LocalDate> availableDates = List.of(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
        when(bookingService.getAvailableDates(propertyId)).thenReturn(availableDates);

        // When
        ResponseEntity<List<LocalDate>> response = bookingController.getAvailableDates(propertyId);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(availableDates, response.getBody());

        verify(bookingService, times(1)).getAvailableDates(propertyId);
    }
}