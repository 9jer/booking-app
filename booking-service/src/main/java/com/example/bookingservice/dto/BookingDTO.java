package com.example.bookingservice.dto;

import com.example.bookingservice.models.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class BookingDTO {
    @NotNull(message = "userId should not be empty!")
    private Long userId;

    @NotNull(message = "propertyId should not be empty!")
    private Long propertyId;

    @NotNull(message = "checkInDate should not be empty!")
    private LocalDate checkInDate;

    @NotNull(message = "checkOutDate should not be empty!")
    private LocalDate checkOutDate;

    @NotNull(message = "status should not be empty!")
    private BookingStatus status;
}
