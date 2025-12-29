package com.example.bookingservice.dto;

import com.example.bookingservice.models.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class GetBookingDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

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

    private BigDecimal totalPrice;
    private GetPropertyDTO property;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
