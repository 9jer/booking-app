package com.example.bookingservice.repositories;

import com.example.bookingservice.models.Booking;
import feign.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Page<Booking> findByPropertyId(Long propertyId, Pageable pageable);

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    Page<Booking> findAll(Pageable pageable);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.propertyId = :propertyId " +
            "AND (b.checkOutDate > :checkIn AND b.checkInDate < :checkOut) AND b.status != 'CANCELLED'")
    Long countOverlappingBookings(@Param("propertyId") Long propertyId,
                                  @Param("checkIn") LocalDate checkIn,
                                  @Param("checkOut") LocalDate checkOut);

    @Query("SELECT b FROM Booking b WHERE b.propertyId = :propertyId " +
            "AND b.checkOutDate >= :today " +
            "AND b.status != com.example.bookingservice.models.BookingStatus.CANCELLED " +
            "ORDER BY b.checkInDate ASC")
    List<Booking> findFutureBookings(@Param("propertyId") Long propertyId, @Param("today") LocalDate today);

    @Query("SELECT b FROM Booking b WHERE b.propertyId = :propertyId AND b.userId = :userId " +
            "AND b.status = 'CONFIRMED' AND b.checkOutDate < CURRENT_DATE")
    List<Booking> findConfirmedBookingByPropertyIdAndUserId(Long propertyId, Long userId);
}
