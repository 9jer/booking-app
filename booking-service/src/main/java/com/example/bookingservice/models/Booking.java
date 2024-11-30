package com.example.bookingservice.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Booking")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long id;

    @Column(name = "user_id")
    @NotNull(message = "userId should not be empty!")
    private Long userId;

    @Column(name = "property_id")
    @NotNull(message = "propertyId should not be empty!")
    private Long propertyId;

    @Column(name = "check_in_date")
    @NotNull(message = "checkInDate should not be empty!")
    private LocalDate checkInDate;

    @Column(name = "check_out_date")
    @NotNull(message = "checkOutDate should not be empty!")
    private LocalDate checkOutDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @NotNull(message = "status should not be empty!")
    private BookingStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
