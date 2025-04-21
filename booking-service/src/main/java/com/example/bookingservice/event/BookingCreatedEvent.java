package com.example.bookingservice.event;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class BookingCreatedEvent {

    private Long bookingId;
    private String email;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
}
