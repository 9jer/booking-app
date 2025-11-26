package com.example.propertyservice.services;

import com.example.propertyservice.dto.GetPropertyDTO;
import com.example.propertyservice.models.Property;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PropertyService {

    List<GetPropertyDTO> findAll();
    GetPropertyDTO getPropertyById(Long id);
    GetPropertyDTO save(Property property, String token);
    GetPropertyDTO updatePropertyById(Long id, Property updatedProperty, String token);
    Boolean isPropertyAvailable(Long propertyId, LocalDate checkIn, LocalDate checkOut);
    List<LocalDate> getAvailableDates(Long propertyId);
    List<GetPropertyDTO> search(String location, BigDecimal minPrice, BigDecimal maxPrice);
    GetPropertyDTO updateAverageRating(Long propertyId, Double averageRating, Long totalReviews);
    Boolean existsById(Long id);
    void delete(Long id, String token);
}