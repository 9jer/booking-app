package com.example.bookingservice.services;

import com.example.bookingservice.client.PropertyClient;
import com.example.bookingservice.client.UserClient;
import com.example.bookingservice.dto.GetPropertyDTO;
import com.example.bookingservice.event.BookingCreatedEvent;
import com.example.bookingservice.event.BookingCreatedEventProducer;
import com.example.bookingservice.models.Booking;
import com.example.bookingservice.models.BookingHistory;
import com.example.bookingservice.models.BookingStatus;
import com.example.bookingservice.repositories.BookingHistoryRepository;
import com.example.bookingservice.repositories.BookingRepository;
import com.example.bookingservice.util.BookingException;
import com.example.bookingservice.util.JwtTokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingHistoryRepository bookingHistoryRepository;

    @Mock
    private PropertyClient propertyClient;

    @Mock
    private UserClient userClient;

    @Mock
    private JwtTokenUtils jwtTokenUtils;

    @Mock
    private BookingCreatedEventProducer producer;

    @Mock
    private BookingService self;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private Booking booking;
    private BookingHistory bookingHistory;

    @BeforeEach
    void setUp() {
        booking = new Booking();
        booking.setId(1L);
        booking.setUserId(1L);
        booking.setPropertyId(1L);
        booking.setCheckInDate(LocalDate.now().plusDays(1));
        booking.setCheckOutDate(LocalDate.now().plusDays(3));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCreatedAt(LocalDateTime.now());

        bookingHistory = new BookingHistory();
        bookingHistory.setId(1L);
        bookingHistory.setBooking(booking);
        bookingHistory.setStatus(BookingStatus.CONFIRMED.name());
        bookingHistory.setChangedAt(LocalDateTime.now());
    }

    @Test
    void getAllBookings_ReturnsListOfBookings() {
        // Given
        when(bookingRepository.findAll()).thenReturn(Collections.singletonList(booking));

        // When
        List<Booking> result = bookingService.getAllBookings();

        // Then
        assertEquals(1, result.size());
        assertEquals(booking, result.get(0));
        verify(bookingRepository, times(1)).findAll();
    }

    @Test
    void getBookingById_BookingExists_ReturnsBooking() {
        // Given
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        // When
        Booking result = bookingService.getBookingById(1L);

        // Then
        assertNotNull(result);
        assertEquals(booking, result);
        verify(bookingRepository, times(1)).findById(1L);
    }

    @Test
    void getBookingById_BookingNotExists_ThrowsException() {
        // Given
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        BookingException exception = assertThrows(BookingException.class,
                () -> bookingService.getBookingById(1L));

        assertEquals("Booking not found", exception.getMessage());
        verify(bookingRepository, times(1)).findById(1L);
    }

    @Test
    void getBookingByPropertyId_ReturnsListOfBookings() {
        // Given
        when(bookingRepository.findByPropertyId(1L)).thenReturn(Collections.singletonList(booking));

        // When
        List<Booking> result = bookingService.getBookingByPropertyId(1L);

        // Then
        assertEquals(1, result.size());
        assertEquals(booking, result.get(0));
        verify(bookingRepository, times(1)).findByPropertyId(1L);
    }

    @Test
    void getBookingHistoryByBookingId_ReturnsListOfHistory() {
        // Given
        when(bookingHistoryRepository.findByBookingId(1L)).thenReturn(Collections.singletonList(bookingHistory));

        // When
        List<BookingHistory> result = bookingService.getBookingHistoryByBookingId(1L);

        // Then
        assertEquals(1, result.size());
        assertEquals(bookingHistory, result.get(0));
        verify(bookingHistoryRepository, times(1)).findByBookingId(1L);
    }

    @Test
    @Transactional
    void createBooking_ValidData_CreatesBooking() {
        // Given
        String token = "valid-token";
        GetPropertyDTO propertyDTO = new GetPropertyDTO();
        propertyDTO.setTitle("Test Property");

        when(propertyClient.propertyExists(1L)).thenReturn(true);
        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);
        when(userClient.userExists(1L)).thenReturn(true);
        when(bookingRepository.countOverlappingBookings(1L, booking.getCheckInDate(), booking.getCheckOutDate()))
                .thenReturn(0L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking savedBooking = invocation.getArgument(0);
            savedBooking.setId(1L);
            return savedBooking;
        });
        when(jwtTokenUtils.getEmail(token)).thenReturn("test@example.com");
        when(propertyClient.getPropertyById(1L)).thenReturn(propertyDTO);

        // When
        Booking result = bookingService.createBooking(booking, token);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(bookingRepository, times(1)).save(booking);
        verify(bookingHistoryRepository, times(1)).save(any(BookingHistory.class));
        verify(producer, times(1)).sendBookingCreatedEvent(anyString(), any(BookingCreatedEvent.class));
    }

    @Test
    @Transactional
    void createBooking_PropertyNotExists_ThrowsException() {
        // Given
        String token = "valid-token";
        when(propertyClient.propertyExists(1L)).thenReturn(false);

        // When & Then
        BookingException exception = assertThrows(BookingException.class,
                () -> bookingService.createBooking(booking, token));

        assertEquals("Property with id 1 not found.", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @Transactional
    void createBooking_UserNotExists_ThrowsException() {
        // Given
        String token = "valid-token";
        when(propertyClient.propertyExists(1L)).thenReturn(true);
        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);
        when(userClient.userExists(1L)).thenReturn(false);

        // When & Then
        BookingException exception = assertThrows(BookingException.class,
                () -> bookingService.createBooking(booking, token));

        assertEquals("User with id 1 not found.", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @Transactional
    void createBooking_PropertyNotAvailable_ThrowsException() {
        // Given
        String token = "valid-token";
        when(propertyClient.propertyExists(1L)).thenReturn(true);
        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);
        when(userClient.userExists(1L)).thenReturn(true);
        when(bookingRepository.countOverlappingBookings(1L, booking.getCheckInDate(), booking.getCheckOutDate()))
                .thenReturn(1L);

        // When & Then
        BookingException exception = assertThrows(BookingException.class,
                () -> bookingService.createBooking(booking, token));

        assertEquals("Property is not available", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @Transactional
    void updateBookingStatus_ValidData_UpdatesStatus() {
        // Given
        String token = "valid-token";

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(jwtTokenUtils.getRoles(token)).thenReturn(List.of("ROLE_ADMIN"));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        // When
        Booking result = bookingService.updateBookingStatus(1L, BookingStatus.CANCELLED, token);

        // Then
        assertNotNull(result);
        assertEquals(BookingStatus.CANCELLED, result.getStatus());
        verify(bookingRepository, times(1)).save(booking);
        verify(bookingHistoryRepository, times(1)).save(any(BookingHistory.class));
    }

    @Test
    void isAvailable_ValidDates_ReturnsTrue() {
        // Given
        when(propertyClient.propertyExists(1L)).thenReturn(true);
        when(bookingRepository.countOverlappingBookings(1L,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)))
                .thenReturn(0L);

        // When
        Boolean result = bookingService.isAvailable(1L,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        // Then
        assertTrue(result);
    }

    @Test
    void isAvailable_InvalidDates_ThrowsException() {
        // When & Then
        BookingException exception = assertThrows(BookingException.class,
                () -> bookingService.isAvailable(1L,
                        LocalDate.now().plusDays(3), LocalDate.now().plusDays(1)));

        assertEquals("Check-in date must be before check-out date.", exception.getMessage());
    }

    @Test
    void isAvailable_PropertyNotExists_ThrowsException() {
        // Given
        when(propertyClient.propertyExists(1L)).thenReturn(false);

        // When & Then
        BookingException exception = assertThrows(BookingException.class,
                () -> bookingService.isAvailable(1L,
                        LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)));

        assertEquals("Property with id 1 not found.", exception.getMessage());
    }

    @Test
    void whetherThereWasABooking_ConfirmedBookingExists_ReturnsTrue() {
        // Given
        when(bookingRepository.findConfirmedBookingByPropertyIdAndUserId(1L, 1L))
                .thenReturn(Collections.singletonList(booking));

        // When
        Boolean result = bookingService.whetherThereWasABooking(1L, 1L);

        // Then
        assertTrue(result);
    }

    @Test
    void whetherThereWasABooking_NoConfirmedBooking_ReturnsFalse() {
        // Given
        when(bookingRepository.findConfirmedBookingByPropertyIdAndUserId(1L, 1L))
                .thenReturn(Collections.emptyList());

        // When
        Boolean result = bookingService.whetherThereWasABooking(1L, 1L);

        // Then
        assertFalse(result);
    }

    @Test
    void getAvailableDates_NoBookings_ReturnsAllDates() {
        // Given
        when(propertyClient.propertyExists(1L)).thenReturn(true);
        when(bookingRepository.findFutureBookings(eq(1L), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusMonths(3);
        long expectedDays = ChronoUnit.DAYS.between(today, endDate);

        // When
        List<LocalDate> result = bookingService.getAvailableDates(1L);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(expectedDays, result.size());
        assertEquals(today, result.get(0));
        assertEquals(endDate.minusDays(1), result.get(result.size()-1));
    }

    @Test
    void getAvailableDates_WithBookings_ReturnsCorrectDates() {
        // Given
        Booking booking1 = new Booking();
        booking1.setCheckInDate(LocalDate.now().plusDays(5));
        booking1.setCheckOutDate(LocalDate.now().plusDays(7));

        Booking booking2 = new Booking();
        booking2.setCheckInDate(LocalDate.now().plusDays(10));
        booking2.setCheckOutDate(LocalDate.now().plusDays(12));

        when(propertyClient.propertyExists(1L)).thenReturn(true);
        when(bookingRepository.findFutureBookings(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of(booking1, booking2));

        // When
        List<LocalDate> result = bookingService.getAvailableDates(1L);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains(LocalDate.now()));
        assertTrue(result.contains(LocalDate.now().plusDays(8)));
        assertTrue(result.contains(LocalDate.now().plusDays(13)));
        assertFalse(result.contains(LocalDate.now().plusDays(6)));
    }

}