package com.example.bookingservice.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class BookingErrorResponse {
    private String message;
    private Long timestamp;
}
