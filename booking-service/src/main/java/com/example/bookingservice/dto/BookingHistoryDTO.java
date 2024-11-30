package com.example.bookingservice.dto;

import com.example.bookingservice.models.Booking;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingHistoryDTO {
    @NotNull(message = "booking should not be empty!")
    private Booking booking;

    @NotNull(message = "status should not be empty!")
    private String status;
}
