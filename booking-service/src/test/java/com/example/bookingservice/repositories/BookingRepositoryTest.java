package com.example.bookingservice.repositories;

import com.example.bookingservice.models.Booking;
import com.example.bookingservice.models.BookingStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:16:///test_db",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class BookingRepositoryTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void countOverlappingBookings_ShouldReturnCorrectCount() {
        Booking booking = new Booking();
        booking.setPropertyId(100L);
        booking.setUserId(1L);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInDate(LocalDate.of(2026, 5, 10));
        booking.setCheckOutDate(LocalDate.of(2026, 5, 20));
        booking.setCreatedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        Long overlap1 = bookingRepository.countOverlappingBookings(
                100L, LocalDate.of(2026, 5, 15), LocalDate.of(2026, 5, 25));
        assertThat(overlap1).isEqualTo(1);

        Long overlap2 = bookingRepository.countOverlappingBookings(
                100L, LocalDate.of(2026, 5, 22), LocalDate.of(2026, 5, 30));
        assertThat(overlap2).isEqualTo(0);

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        Long overlap3 = bookingRepository.countOverlappingBookings(
                100L, LocalDate.of(2026, 5, 15), LocalDate.of(2026, 5, 25));
         assertThat(overlap3).isEqualTo(0);
         assertThat(overlap3).isEqualTo(0);
    }

    @Test
    void findFutureBookings_ShouldReturnOnlyActiveFutureBookingsSorted() {
        LocalDate today = LocalDate.now();
        Booking pastBooking = createBooking(1L, 100L, BookingStatus.CONFIRMED, today.minusDays(10), today.minusDays(5));
        Booking futureBooking1 = createBooking(1L, 100L, BookingStatus.CONFIRMED, today.plusDays(10), today.plusDays(15));
        Booking futureBooking2 = createBooking(2L, 100L, BookingStatus.PENDING, today.plusDays(2), today.plusDays(5));
        Booking cancelledFuture = createBooking(3L, 100L, BookingStatus.CANCELLED, today.plusDays(20), today.plusDays(25));

        bookingRepository.saveAll(List.of(pastBooking, futureBooking1, futureBooking2, cancelledFuture));

        List<Booking> result = bookingRepository.findFutureBookings(100L, today);

        assertThat(result).hasSize(2);

        assertThat(result.get(0).getId()).isEqualTo(futureBooking2.getId());
        assertThat(result.get(1).getId()).isEqualTo(futureBooking1.getId());
    }

    @Test
    void findConfirmedBookingByPropertyIdAndUserId_ShouldReturnOnlyPastConfirmed() {
        LocalDate today = LocalDate.now();
        Booking validPastBooking = createBooking(1L, 100L, BookingStatus.CONFIRMED, today.minusDays(10), today.minusDays(2));
        Booking futureBooking = createBooking(1L, 100L, BookingStatus.CONFIRMED, today.plusDays(5), today.plusDays(10));
        Booking pendingPastBooking = createBooking(1L, 100L, BookingStatus.PENDING, today.minusDays(10), today.minusDays(2));
        Booking wrongUserBooking = createBooking(2L, 100L, BookingStatus.CONFIRMED, today.minusDays(10), today.minusDays(2));

        bookingRepository.saveAll(List.of(validPastBooking, futureBooking, pendingPastBooking, wrongUserBooking));

        List<Booking> result = bookingRepository.findConfirmedBookingByPropertyIdAndUserId(100L, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(validPastBooking.getId());
    }

    private Booking createBooking(Long userId, Long propertyId, BookingStatus status, LocalDate checkIn, LocalDate checkOut) {
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setPropertyId(propertyId);
        booking.setStatus(status);
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setCreatedAt(LocalDateTime.now());
        return booking;
    }
}