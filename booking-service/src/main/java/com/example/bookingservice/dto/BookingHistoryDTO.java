package com.example.bookingservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingHistoryDTO {
    @NotNull(message = "bookingId should not be empty!")
    private Long bookingId;

    @NotNull(message = "status should not be empty!")
    private String status;
}
