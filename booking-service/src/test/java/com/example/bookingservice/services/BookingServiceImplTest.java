package com.example.bookingservice.services;

import com.example.bookingservice.client.PropertyClient;
import com.example.bookingservice.client.UserClient;
import com.example.bookingservice.dto.BookingHistoryDTO;
import com.example.bookingservice.dto.GetBookingDTO;
import com.example.bookingservice.dto.GetPropertyDTO;
import com.example.bookingservice.dto.UserDTO;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.SimpleTransactionStatus;

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
    private ModelMapper modelMapper;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    @Spy
    private BookingServiceImpl bookingService;

    private Booking booking;
    private BookingHistory bookingHistory;
    private GetBookingDTO getBookingDTO;
    private BookingHistoryDTO bookingHistoryDTO;
    private String token = "valid-token";

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

        getBookingDTO = new GetBookingDTO();
        getBookingDTO.setId(1L);

        bookingHistoryDTO = new BookingHistoryDTO();

        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @Test
    void getAllBookings_Admin_ReturnsListOfDTOs() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(jwtTokenUtils.getRoles(token)).thenReturn(List.of("ROLE_ADMIN"));
        when(bookingRepository.findAll(pageable)).thenReturn(new PageImpl<>(Collections.singletonList(booking)));
        when(modelMapper.map(booking, GetBookingDTO.class)).thenReturn(getBookingDTO);

        // When
        Page<GetBookingDTO> result = bookingService.getAllBookings(token, pageable);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals(getBookingDTO, result.getContent().get(0));
        verify(bookingRepository, times(1)).findAll(pageable);
    }

    @Test
    void getBookingById_BookingExists_ReturnsDTO() {
        // Given
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);
        when(modelMapper.map(booking, GetBookingDTO.class)).thenReturn(getBookingDTO);

        // When
        GetBookingDTO result = bookingService.getBookingById(1L, token);

        // Then
        assertNotNull(result);
        assertEquals(getBookingDTO, result);
        verify(bookingRepository, times(1)).findById(1L);
    }

    @Test
    void getBookingById_BookingNotExists_ThrowsException() {
        // Given
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        BookingException exception = assertThrows(BookingException.class,
                () -> bookingService.getBookingById(1L, token));

        assertEquals("Booking not found", exception.getMessage());
        verify(bookingRepository, times(1)).findById(1L);
    }

    @Test
    void getBookingByPropertyId_ReturnsListOfDTOs() {
        // Given
        GetPropertyDTO propertyDTO = new GetPropertyDTO();
        propertyDTO.setOwnerId(1L);
        Pageable pageable = PageRequest.of(0, 10);

        when(jwtTokenUtils.getRoles(token)).thenReturn(List.of("ROLE_OWNER"));
        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);
        when(propertyClient.getPropertyById(1L)).thenReturn(propertyDTO);

        when(bookingRepository.findByPropertyId(1L, pageable)).thenReturn(new PageImpl<>(Collections.singletonList(booking)));
        when(modelMapper.map(booking, GetBookingDTO.class)).thenReturn(getBookingDTO);

        // When
        Page<GetBookingDTO> result = bookingService.getBookingByPropertyId(1L, token, pageable);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals(getBookingDTO, result.getContent().get(0));
        verify(bookingRepository, times(1)).findByPropertyId(1L, pageable);
    }

    @Test
    void getBookingHistoryByBookingId_ReturnsListOfHistoryDTOs() {
        // Given
        when(bookingHistoryRepository.findByBookingId(1L)).thenReturn(Collections.singletonList(bookingHistory));
        when(modelMapper.map(bookingHistory, BookingHistoryDTO.class)).thenReturn(bookingHistoryDTO);

        // When
        List<BookingHistoryDTO> result = bookingService.getBookingHistoryByBookingId(1L);

        // Then
        assertEquals(1, result.size());
        assertEquals(bookingHistoryDTO, result.get(0));
        verify(bookingHistoryRepository, times(1)).findByBookingId(1L);
    }

    @Test
    @Transactional
    void createBooking_ValidData_ReturnsDTO() {
        // Given
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

        when(modelMapper.map(any(Booking.class), eq(GetBookingDTO.class))).thenReturn(getBookingDTO);

        // When
        GetBookingDTO result = bookingService.createBooking(booking, token);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(bookingRepository, times(1)).save(booking);
        verify(bookingHistoryRepository, times(1)).save(any(BookingHistory.class));

        verify(producer, never()).sendBookingCreatedEvent(anyString(), any(BookingCreatedEvent.class));
    }

    @Test
    @Transactional
    void createBooking_PropertyNotExists_ThrowsException() {
        // Given
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
        when(propertyClient.propertyExists(1L)).thenReturn(true);
        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);
        when(userClient.userExists(1L)).thenReturn(true);
        when(bookingRepository.countOverlappingBookings(1L, booking.getCheckInDate(), booking.getCheckOutDate()))
                .thenReturn(1L);

        // When & Then
        BookingException exception = assertThrows(BookingException.class,
                () -> bookingService.createBooking(booking, token));

        assertEquals("Property is not available for selected dates.", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @Transactional
    void updateBookingStatus_ValidData_ReturnsDTO() {
        // Given
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(jwtTokenUtils.getRoles(token)).thenReturn(List.of("ROLE_ADMIN"));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        when(modelMapper.map(booking, GetBookingDTO.class)).thenReturn(getBookingDTO);

        // When
        GetBookingDTO result = bookingService.updateBookingStatus(1L, BookingStatus.CANCELLED, token);

        // Then
        assertNotNull(result);
        verify(bookingRepository, times(1)).save(booking);
        verify(bookingHistoryRepository, times(1)).save(any(BookingHistory.class));
    }

    @Test
    void isAvailable_ValidDates_ReturnsTrue() {
        // Given
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

    @Test
    void initiatePayment_ValidBooking_NoException() {
        // Given
        booking.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);

        // When & Then
        assertDoesNotThrow(() -> bookingService.initiatePayment(1L, token));

        assertEquals(BookingStatus.AWAITING_PAYMENT, booking.getStatus());
    }

    @Test
    void completePayment_ValidBooking_UpdatesStatusToPaid() {
        // Given
        booking.setStatus(BookingStatus.AWAITING_PAYMENT);

        GetPropertyDTO propertyDTO = new GetPropertyDTO();
        propertyDTO.setTitle("Luxury Villa");

        UserDTO userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setEmail("test@example.com");
        userDTO.setUsername("testuser");

        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(bookingService).executeAfterCommit(any());

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingHistoryRepository.save(any(BookingHistory.class))).thenReturn(new BookingHistory());
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        when(propertyClient.getPropertyById(1L)).thenReturn(propertyDTO);
        when(userClient.getUserById(1L)).thenReturn(userDTO);

        // When
        bookingService.completePayment(1L);

        // Then
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(bookingRepository, times(1)).save(booking);

        verify(producer, times(1)).sendBookingCreatedEvent(anyString(), any(BookingCreatedEvent.class));
    }

    @Test
    @Transactional
    void cancelBooking_OwnerOrAdmin_CancelsBooking() {
        // Given
        booking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(jwtTokenUtils.getRoles(token)).thenReturn(List.of("ROLE_ADMIN"));

        when(bookingRepository.save(booking)).thenReturn(booking);

        // When
        bookingService.cancelBooking(1L, token);

        // Then
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(bookingHistoryRepository).save(any(BookingHistory.class));
    }

    @Test
    void cancelBooking_NotAuthorized_ThrowsException() {
        // Given
        booking.setUserId(999L);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);
        when(jwtTokenUtils.getRoles(token)).thenReturn(List.of("ROLE_USER"));

        // When & Then
        BookingException exception = assertThrows(BookingException.class,
                () -> bookingService.cancelBooking(1L, token));
        assertEquals("You are not authorized to cancel this booking.", exception.getMessage());
    }

    @Test
    void cancelBooking_AlreadyCancelled_ThrowsException() {
        // Given
        booking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);
        when(jwtTokenUtils.getRoles(token)).thenReturn(List.of("ROLE_ADMIN"));

        // When & Then
        BookingException exception = assertThrows(BookingException.class,
                () -> bookingService.cancelBooking(1L, token));
        assertEquals("Booking is already cancelled.", exception.getMessage());
    }

    @Test
    void getUserRecentBookings_ReturnsList() {
        // Given
        when(jwtTokenUtils.getUserId(token)).thenReturn(1L);
        when(bookingRepository.findTop5ByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(booking));
        when(modelMapper.map(booking, GetBookingDTO.class)).thenReturn(getBookingDTO);

        // When
        List<GetBookingDTO> result = bookingService.getUserRecentBookings(token);

        // Then
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }
}