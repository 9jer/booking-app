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
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final BookingHistoryRepository bookingHistoryRepository;
    private final PropertyClient propertyClient;
    private final UserClient userClient;
    private final JwtTokenUtils jwtTokenUtils;
    private final BookingCreatedEventProducer producer;

    private static final int MAX_BOOKING_WINDOW_MONTHS = 3;

    @Override
    public List<Booking> getAllBookings(String token) {
        List<String> roles = jwtTokenUtils.getRoles(token);
        if (roles.contains("ROLE_ADMIN")) {
            return bookingRepository.findAll();
        }

        Long currentUserId = jwtTokenUtils.getUserId(token);
        return bookingRepository.findByUserId(currentUserId);
    }

    @Override
    public Booking getBookingById(Long bookingId, String token) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found"));

        Long currentUserId = jwtTokenUtils.getUserId(token);
        List<String> roles = jwtTokenUtils.getRoles(token);

        if (roles.contains("ROLE_ADMIN")) {
            return booking;
        }

        if (booking.getUserId().equals(currentUserId)) {
            return booking;
        }

        GetPropertyDTO property = propertyClient.getPropertyById(booking.getPropertyId());
        if (property.getOwnerId().equals(currentUserId)) {
            return booking;
        }

        throw new BookingException("You do not have permission to view this booking.");
    }

    @Override
    public List<Booking> getBookingByPropertyId(Long propertyId, String token) {
        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains("ROLE_ADMIN")) {
            Long currentUserId = jwtTokenUtils.getUserId(token);
            GetPropertyDTO property = propertyClient.getPropertyById(propertyId);

            if (property == null) {
                throw new BookingException("Property not found");
            }

            if (!property.getOwnerId().equals(currentUserId)) {
                throw new BookingException("You do not have permission to view bookings for this property.");
            }
        }

        return bookingRepository.findByPropertyId(propertyId);
    }

    @Override
    public List<BookingHistory> getBookingHistoryByBookingId(Long bookingId) {
        return bookingHistoryRepository.findByBookingId(bookingId);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @CacheEvict(value = "availableDates", key = "#booking.propertyId")
    public Booking createBooking(Booking booking, String token) {
        booking.setId(null);

        if (booking.getStatus() == null) {
            booking.setStatus(BookingStatus.PENDING);
        }

        validateBookingDates(booking.getCheckInDate(), booking.getCheckOutDate());

        Boolean propertyExists = propertyClient.propertyExists(booking.getPropertyId());
        if (propertyExists == null || !propertyExists) {
            throw new BookingException("Property with id " + booking.getPropertyId() + " not found.");
        }

        booking.setUserId(jwtTokenUtils.getUserId(token));

        Boolean userExists = userClient.userExists(booking.getUserId());
        if (userExists == null || !userExists) {
            throw new BookingException("User with id " + booking.getUserId() + " not found.");
        }

        if (!isAvailable(booking.getPropertyId(), booking.getCheckInDate(), booking.getCheckOutDate())) {
            throw new BookingException("Property is not available for selected dates.");
        }

        booking.setCreatedAt(LocalDateTime.now());
        Booking savedBooking = bookingRepository.save(booking);
        saveHistory(savedBooking, booking.getStatus());

        if (savedBooking.getStatus() == BookingStatus.CONFIRMED) {
            sendNotification(savedBooking, token);
        }

        return savedBooking;
    }

    @Override
    @Transactional
    @CacheEvict(value = "availableDates", key = "#result.propertyId")
    public Booking updateBookingStatus(Long bookingId, BookingStatus bookingStatus, String token) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found"));

        if (booking.getStatus() == bookingStatus) {
            return booking;
        }

        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains("ROLE_ADMIN")) {
            Long currentUserId = jwtTokenUtils.getUserId(token);
            GetPropertyDTO property = propertyClient.getPropertyById(booking.getPropertyId());

            if (!property.getOwnerId().equals(currentUserId)) {
                throw new BookingException("You are not authorized to manage bookings for this property.");
            }
        }

        booking.setStatus(bookingStatus);
        booking.setUpdatedAt(LocalDateTime.now());
        saveHistory(booking, bookingStatus);

        Booking updatedBooking = bookingRepository.save(booking);

        if (updatedBooking.getStatus() == BookingStatus.CONFIRMED) {
            sendNotification(updatedBooking, token);
        }

        return updatedBooking;
    }

    @Override
    public Boolean isAvailable(Long propertyId, LocalDate checkIn, LocalDate checkOut) {
        validateBookingDates(checkIn, checkOut);

        Boolean propertyExists = propertyClient.propertyExists(propertyId);
        if (propertyExists == null || !propertyExists) {
            throw new BookingException("Property with id " + propertyId + " not found.");
        }

        return bookingRepository.countOverlappingBookings(propertyId, checkIn, checkOut) == 0;
    }

    @Override
    public Boolean whetherThereWasABooking(Long propertyId, Long userId) {
        return !bookingRepository.findConfirmedBookingByPropertyIdAndUserId(propertyId, userId).isEmpty();
    }

    @Override
    @Cacheable(value = "availableDates", key = "#propertyId")
    public List<LocalDate> getAvailableDates(Long propertyId) {
        Boolean propertyExists = propertyClient.propertyExists(propertyId);
        if (propertyExists == null || !propertyExists) {
            throw new BookingException("Property with id " + propertyId + " not found.");
        }

        List<Booking> bookings = bookingRepository.findFutureBookings(propertyId, LocalDate.now());

        List<LocalDate> availableDates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusMonths(MAX_BOOKING_WINDOW_MONTHS);

        if (bookings.isEmpty()) {
            for (LocalDate date = today; date.isBefore(maxDate); date = date.plusDays(1)) {
                availableDates.add(date);
            }
            return availableDates;
        }

        LocalDate currentDate = today;

        if (bookings.get(0).getCheckInDate().isAfter(currentDate)) {
            LocalDate end = bookings.get(0).getCheckInDate().minusDays(1);
            while (!currentDate.isAfter(end) && currentDate.isBefore(maxDate)) {
                availableDates.add(currentDate);
                currentDate = currentDate.plusDays(1);
            }
        }

        for (int i = 0; i < bookings.size() - 1; i++) {
            LocalDate endOfCurrent = bookings.get(i).getCheckOutDate().plusDays(1);
            LocalDate startOfNext = bookings.get(i + 1).getCheckInDate().minusDays(1);

            if (endOfCurrent.isAfter(maxDate)) break;

            while (!endOfCurrent.isAfter(startOfNext) && endOfCurrent.isBefore(maxDate)) {
                availableDates.add(endOfCurrent);
                endOfCurrent = endOfCurrent.plusDays(1);
            }
        }

        LocalDate lastBookingEnd = bookings.get(bookings.size() - 1).getCheckOutDate().plusDays(1);
        while (lastBookingEnd.isBefore(maxDate)) {
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

    private void validateBookingDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn.isAfter(checkOut) || checkIn.isEqual(checkOut)) {
            throw new BookingException("Check-in date must be before check-out date.");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new BookingException("Cannot book dates in the past.");
        }
        if (checkOut.isAfter(LocalDate.now().plusMonths(MAX_BOOKING_WINDOW_MONTHS))) {
            throw new BookingException("Cannot book more than " + MAX_BOOKING_WINDOW_MONTHS + " months in advance.");
        }
    }

    private void sendNotification(Booking booking, String token) {
        BookingCreatedEvent bookingCreatedEvent = new BookingCreatedEvent();
        bookingCreatedEvent.setBookingId(booking.getId());
        bookingCreatedEvent.setEmail(jwtTokenUtils.getEmail(token));

        try {
            String propertyTitle = propertyClient.getPropertyById(booking.getPropertyId()).getTitle();
            bookingCreatedEvent.setPropertyName(propertyTitle);
        } catch (Exception e) {
            bookingCreatedEvent.setPropertyName("Unknown Property");
        }

        bookingCreatedEvent.setCheckInDate(booking.getCheckInDate().toString());
        bookingCreatedEvent.setCheckOutDate(booking.getCheckOutDate().toString());

        producer.sendBookingCreatedEvent("booking-created", bookingCreatedEvent);
    }
}