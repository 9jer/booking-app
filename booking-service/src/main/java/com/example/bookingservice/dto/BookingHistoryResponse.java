package com.example.bookingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class BookingHistoryResponse {
    private List<BookingHistoryDTO> history;
}
