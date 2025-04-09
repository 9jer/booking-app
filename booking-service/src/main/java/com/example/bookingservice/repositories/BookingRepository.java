package com.example.bookingservice.repositories;

import com.example.bookingservice.models.Booking;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByPropertyId(Long propertyId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.propertyId = :propertyId " +
            "AND (b.checkOutDate >= :checkIn AND b.checkInDate <= :checkOut) AND b.status != 'CANCELLED'")
    Long countOverlappingBookings(Long propertyId, LocalDate checkIn, LocalDate checkOut);

    @Query("SELECT b FROM Booking b WHERE b.propertyId = :propertyId ORDER BY b.checkInDate ASC")
    List<Booking> findBookingsByPropertyOrdered(@Param("propertyId") Long propertyId);

    @Query("SELECT b FROM Booking b WHERE b.propertyId = :propertyId AND b.userId = :userId and b.status <> 'PENDING'")
    List<Booking> findConfirmedBookingByPropertyIdAndUserId(Long propertyId, Long userId);
}
