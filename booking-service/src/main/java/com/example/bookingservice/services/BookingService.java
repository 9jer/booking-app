package com.example.bookingservice.services;

import com.example.bookingservice.client.PropertyClient;
import com.example.bookingservice.client.UserClient;
import com.example.bookingservice.models.Booking;
import com.example.bookingservice.models.BookingHistory;
import com.example.bookingservice.models.BookingStatus;
import com.example.bookingservice.repositories.BookingHistoryRepository;
import com.example.bookingservice.repositories.BookingRepository;
import com.example.bookingservice.util.BookingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BookingService {
    private final BookingRepository bookingRepository;
    private final BookingHistoryRepository bookingHistoryRepository;
    private final PropertyClient propertyClient;
    private final UserClient userClient;

    public BookingService(BookingRepository bookingRepository, BookingHistoryRepository bookingHistoryRepository, PropertyClient propertyClient, UserClient userClient) {
        this.bookingRepository = bookingRepository;
        this.bookingHistoryRepository = bookingHistoryRepository;
        this.propertyClient = propertyClient;
        this.userClient = userClient;
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Booking getBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(()-> new BookingException("Booking not found"));
    }

    public List<Booking> getBookingByPropertyId(Long propertyId) {
        return bookingRepository.findByPropertyId(propertyId);
    }

    public List<BookingHistory> getBookingHistoryByBookingId(Long bookingId) {
        return bookingHistoryRepository.findByBookingId(bookingId);
    }

    @Transactional
    public Booking createBooking(Booking booking, String jwtToken) {

        Boolean propertyExists = propertyClient.checkPropertyExists(booking.getPropertyId(), jwtToken).block();
        if (propertyExists == null || !propertyExists) {
            throw new BookingException("Property with id " + booking.getPropertyId() + " not found.");
        }

        Boolean userExists = userClient.checkUserExists(booking.getUserId(), jwtToken).block();

        if (userExists == null || !userExists) {
            throw new BookingException("User with id " + booking.getUserId()
                    + " not found.");
        }

        if(!isAvailable(booking.getPropertyId(), booking.getCheckInDate(), booking.getCheckOutDate(), jwtToken)){
            throw new BookingException("Property is not available");
        }

        booking.setStatus(BookingStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());
        Booking savedBooking = bookingRepository.save(booking);
        saveHistory(savedBooking, BookingStatus.PENDING);
        return savedBooking;
    }

    @Transactional
    public Booking updateBookingStatus(Long bookingId, BookingStatus bookingStatus) {
        Booking booking = getBookingById(bookingId);
        booking.setStatus(bookingStatus);
        booking.setUpdatedAt(LocalDateTime.now());
        saveHistory(booking, bookingStatus);
        return bookingRepository.save(booking);
    }

    public Boolean isAvailable(Long propertyId, LocalDate checkIn, LocalDate checkOut, String jwtToken) {
        if (checkIn.isAfter(checkOut) || checkIn.isEqual(checkOut)) {
            throw new BookingException("Check-in date must be before check-out date.");
        }

        Boolean propertyExists = propertyClient.checkPropertyExists(propertyId, jwtToken).block();
        if (propertyExists == null || !propertyExists) {
            throw new BookingException("Property with id " + propertyId + " not found.");
        }

        return bookingRepository.countOverlappingBookings(propertyId, checkIn, checkOut) == 0;
    }

    public Boolean whetherThereWasABooking(Long propertyId, Long userId){
        return !(bookingRepository.findBookingByPropertyIdAndUserId(propertyId, userId).isEmpty());
    }

    public List<LocalDate> getAvailableDates(Long propertyId) {
        List<Booking> bookings = bookingRepository.findBookingsByPropertyOrdered(propertyId);

        List<LocalDate> availableDates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusMonths(3);

        if (bookings.isEmpty()) {
            for (LocalDate date = today; date.isBefore(maxDate); date = date.plusDays(1)) {
                availableDates.add(date);
            }
            return availableDates;
        }

        if (bookings.get(0).getCheckInDate().isAfter(today)) {
            LocalDate start = today;
            LocalDate end = bookings.get(0).getCheckInDate().minusDays(1);
            while (!start.isAfter(end)) {
                availableDates.add(start);
                start = start.plusDays(1);
            }
        }

        for (int i = 0; i < bookings.size() - 1; i++) {
            LocalDate endOfCurrent = bookings.get(i).getCheckOutDate().plusDays(1);
            LocalDate startOfNext = bookings.get(i + 1).getCheckInDate().minusDays(1);
            while (!endOfCurrent.isAfter(startOfNext)) {
                availableDates.add(endOfCurrent);
                endOfCurrent = endOfCurrent.plusDays(1);
            }
        }

        LocalDate lastBookingEnd = bookings.get(bookings.size() - 1).getCheckOutDate().plusDays(1);
        while (!lastBookingEnd.isAfter(maxDate)) {
            availableDates.add(lastBookingEnd);
            lastBookingEnd = lastBookingEnd.plusDays(1);
        }

        return availableDates;
    }

    private void saveHistory(Booking booking, BookingStatus bookingStatus) {
        BookingHistory bookingHistory = new BookingHistory();
        bookingHistory.setBooking(booking);
        bookingHistory.setChangedAt(LocalDateTime.now());
        bookingHistory.setStatus(bookingStatus.name());
        bookingHistoryRepository.save(bookingHistory);
    }
}
