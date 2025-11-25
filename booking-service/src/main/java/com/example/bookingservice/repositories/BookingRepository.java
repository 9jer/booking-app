package com.example.bookingservice.repositories;

import com.example.bookingservice.models.Booking;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByPropertyId(Long propertyId);
    List<Booking> findByUserId(Long userId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.propertyId = :propertyId " +
            "AND (b.checkOutDate >= :checkIn AND b.checkInDate <= :checkOut) AND b.status != 'CANCELLED'")
    Long countOverlappingBookings(@Param("propertyId") Long propertyId,
                                  @Param("checkIn") LocalDate checkIn,
                                  @Param("checkOut") LocalDate checkOut);

    @Query("SELECT b FROM Booking b WHERE b.propertyId = :propertyId AND b.checkOutDate >= :today ORDER BY b.checkInDate ASC")
    List<Booking> findFutureBookings(@Param("propertyId") Long propertyId, @Param("today") LocalDate today);

    @Query("SELECT b FROM Booking b WHERE b.propertyId = :propertyId AND b.userId = :userId " +
            "AND b.status = 'CONFIRMED' AND b.checkOutDate < CURRENT_DATE")
    List<Booking> findConfirmedBookingByPropertyIdAndUserId(Long propertyId, Long userId);
}
