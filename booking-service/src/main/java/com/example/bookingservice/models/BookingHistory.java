package com.example.bookingservice.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Booking_History")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class BookingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", referencedColumnName = "booking_id")
    @NotNull(message = "booking should not be empty!")
    private Booking booking;

    @Column(name = "status")
    @NotNull(message = "status should not be empty!")
    private String status;

    @Column(name = "changed_at")
    private LocalDateTime changedAt;
}
