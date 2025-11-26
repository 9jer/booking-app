package com.example.bookingservice.services;

import com.example.bookingservice.dto.GetBookingDTO;
import com.example.bookingservice.dto.BookingHistoryDTO;
import com.example.bookingservice.models.Booking;
import com.example.bookingservice.models.BookingStatus;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {
    List<GetBookingDTO> getAllBookings(String token);
    GetBookingDTO getBookingById(Long bookingId, String token);
    List<GetBookingDTO> getBookingByPropertyId(Long propertyId, String token);
    List<BookingHistoryDTO> getBookingHistoryByBookingId(Long bookingId);
    GetBookingDTO createBooking(Booking booking, String token);
    GetBookingDTO updateBookingStatus(Long bookingId, BookingStatus bookingStatus, String token);
    Boolean isAvailable(Long propertyId, LocalDate checkIn, LocalDate checkOut);
    Boolean whetherThereWasABooking(Long propertyId, Long userId);
    List<LocalDate> getAvailableDates(Long propertyId);
}