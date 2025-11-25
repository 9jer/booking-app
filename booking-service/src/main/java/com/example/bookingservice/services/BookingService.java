package com.example.bookingservice.services;

import com.example.bookingservice.models.Booking;
import com.example.bookingservice.models.BookingHistory;
import com.example.bookingservice.models.BookingStatus;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {
    List<Booking> getAllBookings(String token);
    Booking getBookingById(Long bookingId, String token);
    List<Booking> getBookingByPropertyId(Long propertyId, String token);
    List<BookingHistory> getBookingHistoryByBookingId(Long bookingId);
    Booking createBooking(Booking booking, String token);
    Booking updateBookingStatus(Long bookingId, BookingStatus bookingStatus, String token);
    Boolean isAvailable(Long propertyId, LocalDate checkIn, LocalDate checkOut);
    Boolean whetherThereWasABooking(Long propertyId, Long userId);
    List<LocalDate> getAvailableDates(Long propertyId);
}
