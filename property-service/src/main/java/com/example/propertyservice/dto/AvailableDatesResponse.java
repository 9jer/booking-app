package com.example.propertyservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class AvailableDatesResponse {
    private List<LocalDate> availableDates;
}
