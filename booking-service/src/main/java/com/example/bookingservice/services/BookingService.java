package com.example.bookingservice.services;

import com.example.bookingservice.dto.GetBookingDTO;
import com.example.bookingservice.dto.BookingHistoryDTO;
import com.example.bookingservice.models.Booking;
import com.example.bookingservice.models.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {
    Page<GetBookingDTO> getAllBookings(String token, Pageable pageable);
    GetBookingDTO getBookingById(Long bookingId, String token);
    Page<GetBookingDTO> getBookingByPropertyId(Long propertyId, String token, Pageable pageable);
    List<BookingHistoryDTO> getBookingHistoryByBookingId(Long bookingId);
    List<GetBookingDTO> getUserRecentBookings(String jwtToken);
    GetBookingDTO createBooking(Booking booking, String token);
    GetBookingDTO updateBookingStatus(Long bookingId, BookingStatus bookingStatus, String token);
    void initiatePayment(Long bookingId, String token);
    void completePayment(Long bookingId);
    Boolean isAvailable(Long propertyId, LocalDate checkIn, LocalDate checkOut);
    Boolean whetherThereWasABooking(Long propertyId, Long userId);
    List<LocalDate> getAvailableDates(Long propertyId);
    void cancelBooking(Long bookingId, String token);
}