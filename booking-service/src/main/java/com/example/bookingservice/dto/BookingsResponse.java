package com.example.bookingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class BookingsResponse {
    private List<BookingDTO> bookings;
}
